package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.job.JobPostRequest;
import hiring_project.shadow_assistant.entity.Job;
import hiring_project.shadow_assistant.entity.User;
import hiring_project.shadow_assistant.error.ResourceNotFoundException;
import hiring_project.shadow_assistant.repository.JobApplicationRepository;
import hiring_project.shadow_assistant.repository.JobRepository;
import hiring_project.shadow_assistant.repository.UserRepository;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.FileIngestionService;
import hiring_project.shadow_assistant.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static hiring_project.shadow_assistant.enums.Role.CANDIDATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final FileIngestionService fileIngestionService;
    private final VectorStore vectorStore;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional
    public Job createJob(JobPostRequest request, MultipartFile file) throws IOException {
        Long recruiterId = authUtil.getLoggedInUserId();
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        // 1. Get Text Content (From File OR Form)
        String pdfContent = "";
        byte[] fileBytes = null;
        String fileName = null;
        String contentType = null;

        if (file != null && !file.isEmpty()) {
            // Reuse the parser from Ingestion Service
            pdfContent = fileIngestionService.extractTextFromFile(file);
            // 2. ⚡ Capture File Data for DB Storage
            fileBytes = file.getBytes();
            fileName = file.getOriginalFilename();
            contentType = file.getContentType();
        } else {
            pdfContent = request.description();
        }

        // 2. SAVE STRUCTURED DATA TO SQL
        Job job = Job.builder()
                .recruiter(recruiter)
                .title(request.title())
                .location(request.location())
                .salaryRange(request.salaryRange())
                .experienceLevel(request.experienceLevel())
                .requiredSkills(request.requiredSkills())
                .interviewType(request.interviewType())
                .description(request.description())// Form Description
                .fullTextFromPdf(pdfContent)       // Saved as Simple File (Text)
                .jdFile(fileBytes)
                .jdFileName(fileName)
                .jdContentType(contentType)
                .build();

        Job savedJob = jobRepository.save(job);
        log.info("Job ID {} saved to SQL.", savedJob.getId());
        savedJob.setApplicants(0);

        // 3. Delegate to Ingestion Service for AI & Vectors
        // We pass the Job ID so the vector is tagged "JOB_{userId}"
        fileIngestionService.ingestJobContent(pdfContent, savedJob.getId());

        return savedJob;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getMyJobs() {
        Long recruiterId = authUtil.getLoggedInUserId();
        List<Job> jobs = jobRepository.findByRecruiterId(recruiterId);

        // ⚡ FIX: Populate applicant count here too
        jobs.forEach(job -> {
            long count = jobApplicationRepository.countByJobId(job.getId());
            job.setApplicants(count);
        });

        return jobs;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        List<Job> jobs = jobRepository.findAll();

        // ⚡ FIX: Populate applicant count for every job
        jobs.forEach(job -> {
            long count = jobApplicationRepository.countByJobId(job.getId());
            job.setApplicants(count);
        });

        return jobs;
    }

    // ⚡ FIX: Intelligent Role-Based Access Control
    @Override
    @Transactional(readOnly = true)
    public Job getJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Long userId = authUtil.getLoggedInUserId();

        User currentUser = userRepository.findById(userId).orElseThrow();

        boolean isCandidate = currentUser.getRole().equals(CANDIDATE);
        if (isCandidate) {
            // ✅ Candidates can view ANY job (to apply)
            return job;
        } else {
            // 🔒 Recruiters can ONLY view THEIR OWN jobs
            if (!job.getRecruiter().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized: You do not own this job posting");
            }
            // Populate stats only for the owner
            job.setApplicants(jobApplicationRepository.countByJobId(jobId));
            return job;
        }
    }

    // --- NEW: Update Job ---
    @Transactional
    public Job updateJob(Long jobId, JobPostRequest request) {
        Job job = getJobById(jobId); // Performs auth check

        // 1. Update SQL Fields
        job.setTitle(request.title());
        job.setLocation(request.location());
        job.setSalaryRange(request.salaryRange());
        job.setExperienceLevel(request.experienceLevel());
        job.setRequiredSkills(request.requiredSkills());
        job.setInterviewType(request.interviewType());
        job.setDescription(request.description());

        // If they change the description text, we update that.
        Job updatedJob = jobRepository.save(job);

        // 2. Sync AI (Delete Old Vectors + Add New)
        deleteJobVectors(jobId);

        // Use the new description (or old PDF text if they didn't upload a new one)
        // For this flow, let's assume we re-ingest the Description + existing PDF Text
        String contentToVectorize = request.description() + "\n" + job.getFullTextFromPdf();
        fileIngestionService.ingestJobContent(contentToVectorize, jobId);

        return updatedJob;
    }

    // --- NEW: Delete Job ---
    @Transactional
    public void deleteJob(Long jobId) {
        Job job = getJobById(jobId); // Performs auth check

        // 1. Delete Vectors first
        deleteJobVectors(jobId);

        // 2. Delete from SQL
        jobRepository.delete(job);
    }

    // --- Helper: Clean up AI Memory ---
    private void deleteJobVectors(Long jobId) {
        try {
            String referenceId = "JOB_" + jobId;
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            // 1. Find all vectors for this Job
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .topK(100) // Assuming a job won't have >100 chunks
                            .filterExpression(b.eq("referenceId", referenceId).build())
                            .build()
            );

            // 2. Collect IDs
            List<String> ids = docs.stream().map(Document::getId).collect(Collectors.toList());

            // 3. Delete
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
                log.info("Deleted {} vectors for Job ID {}", ids.size(), jobId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete vectors for Job {}: {}", jobId, e.getMessage());
        }
    }
}