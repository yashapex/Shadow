package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;
import hiring_project.shadow_assistant.dto.job.ScheduleInterviewRequest;
import hiring_project.shadow_assistant.entity.*;
import hiring_project.shadow_assistant.enums.ApplicationStatus;
import hiring_project.shadow_assistant.enums.Verdict;
import hiring_project.shadow_assistant.error.ResourceNotFoundException;
import hiring_project.shadow_assistant.repository.*;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.EliminatorService;
import hiring_project.shadow_assistant.service.JobApplicationService;
import hiring_project.shadow_assistant.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient; // ⚡ Import AI Client
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hiring_project.shadow_assistant.dto.ai.InterviewAnalysisResponse; // DTO for AI output
import hiring_project.shadow_assistant.dto.ai.InterviewSummaryResponse;   // DTO for Final Summary

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EliminatorService eliminatorService;
    private final AuthUtil authUtil;
    private final LiveInterviewRepository liveInterviewRepository;

    // ⚡ FIX 1: Inject ChatClient to execute AI calls
    private final ChatClient chatClient;
    private final InterviewAnalysisRepository interviewAnalysisRepository;



    @Override
    @Transactional
    public void applyForJob(Long jobId) {
        Long candidateId = authUtil.getLoggedInUserId();

        if (jobApplicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new RuntimeException("You have already applied for this job.");
        }

        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        CandidateProfile profile = candidateProfileRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Please complete your profile first."));

        if (profile.getResumeFile() == null) {
            throw new RuntimeException("No resume found. Please upload your resume in Profile Settings.");
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .candidate(candidate)
                .status(ApplicationStatus.APPLIED)
                .matchScore(0)
                .resumeFile(profile.getResumeFile())
                .resumeFileName(profile.getResumeFileName())
                .resumeContentType(profile.getResumeContentType())
                .appliedAt(LocalDateTime.now())
                .build();

        JobApplication savedApp = jobApplicationRepository.save(application);
        log.info("Application saved: ID {}", savedApp.getId());

        try {
            eliminatorService.evaluateCandidate(savedApp.getId());
        } catch (Exception e) {
            log.error("AI Evaluation failed triggering: {}", e.getMessage());
        }
    }

    @Override
    public List<JobApplication> getMyApplications() {
        return jobApplicationRepository.findByCandidateId(authUtil.getLoggedInUserId());
    }

    // --- ⚡ FIX 2: Corrected Pre-Interview Logic ---
    @Override
    public InterviewQuestionsResponse generatePreInterviewQuestions(Long applicationId) {
        // 1. Fetch Application & Data
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Optional: Strict status check
        // if (!application.getStatus().equals(ApplicationStatus.SCREENING)) {
        //     throw new RuntimeException("Interview not available for this application status");
        // }

        Job job = application.getJob();
        CandidateProfile candidate = candidateProfileRepository.findById(application.getCandidate().getId())
                .orElseThrow(() -> new RuntimeException("Candidate profile not found"));

        // 2. Prepare Data for Prompt
        // Since we don't have the full resume text here easily, we construct a summary
        String resumeSummary = "Skills: " + candidate.getSkills() + "\nBio: " + candidate.getBio();

        // 3. Build Prompt using your Utility
        String prompt = PromptUtil.getQuestionGeneratorPrompt(job.getDescription(), resumeSummary);

        // 4. Call AI & Return Structured Data directly
        // This maps the AI's JSON output directly to your InterviewQuestionsResponse record
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(InterviewQuestionsResponse.class);
    }

    @Override
    @Transactional
    public void submitInterview(Long applicationId, Map<String, Object> results) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(ApplicationStatus.SHORTLISTED);
        // application.setInterviewData(results.toString()); // If you have this field
        jobApplicationRepository.save(application);
    }

    @Override
    public List<JobApplication> getApplicationsForJob(Long jobId) {
        Long currentRecruiterId = authUtil.getLoggedInUserId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getRecruiter().getId().equals(currentRecruiterId)) {
            throw new RuntimeException("Unauthorized");
        }
        return jobApplicationRepository.findByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplication getApplicationById(Long applicationId) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        Long currentUserId = authUtil.getLoggedInUserId();
        Long candidateId = app.getCandidate().getId();
        Long recruiterId = app.getJob().getRecruiter().getId();

        if (!currentUserId.equals(candidateId) && !currentUserId.equals(recruiterId)) {
            throw new RuntimeException("Unauthorized");
        }
        return app;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplication> getApplicationsForRecruiter() {
        Long recruiterId = authUtil.getLoggedInUserId();
        return jobApplicationRepository.findByJobRecruiterId(recruiterId);
    }

    // Missing downloadResume implementation from previous context
    @Override
    @Transactional(readOnly = true)
    public JobApplication downloadResume(Long applicationId) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Access bytes to force load
        if (app.getResumeFile() != null) {
            int size = app.getResumeFile().length;
        }
        return app;
    }

    @Override
    @Transactional
    public void submitAnswer(Long applicationId, String question, String answer) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // 1. Analyze with AI
        String prompt = PromptUtil.getDepthDetectorPrompt(question, answer);
        InterviewAnalysisResponse aiResult = chatClient.prompt().user(prompt).call().entity(InterviewAnalysisResponse.class);

        // 2. ⚡ SAVE TO INTERVIEW_ANALYSIS TABLE
        InterviewAnalysis analysis = InterviewAnalysis.builder()
                .application(app)
                .question(question)
                .candidateAnswer(answer)
                .technicalScore(aiResult.technicalScore())
                .communicationScore(aiResult.communicationScore())
                .isGeneric(aiResult.isGeneric())
                .aiFeedback(aiResult.analysis())
                .build();

        interviewAnalysisRepository.save(analysis); // <--- THIS SAVES THE DATA
    }

    // 2. ⚡ FINALIZE INTERVIEW (Called after all questions are done)
    @Override
    @Transactional
    public void finalizeInterview(Long applicationId) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<InterviewAnalysis> answers = interviewAnalysisRepository.findByApplicationId(applicationId);
        String summaryPrompt = getSummaryPrompt(answers);

        InterviewSummaryResponse finalVerdict = chatClient.prompt()
                .user(summaryPrompt)
                .call()
                .entity(InterviewSummaryResponse.class);

        if (finalVerdict != null) {
            // 1. Map Scores
            app.setInterviewScore(finalVerdict.averageScore());

            // 2. ⚡ FIX: Map fields individually (Do not combine them string format)
            app.setInterviewSummary(finalVerdict.interviewSummary());
            app.setKeyObservation(finalVerdict.keyObservation());
            app.setRedFlags(finalVerdict.redFlags());

            // 3. Set Verdict
            app.setInterviewVerdict(
                    "Highly Recommended".equalsIgnoreCase(finalVerdict.aiRecommendation()) ||
                            "Worth Interviewing".equalsIgnoreCase(finalVerdict.aiRecommendation())
                            ? Verdict.ACCEPTED : Verdict.REJECTED
            );

            // 4. Update Status and Save
            app.setStatus(ApplicationStatus.AIINTERVIEWED);
            jobApplicationRepository.save(app);

            log.info("Finalized interview for App ID {}. Score: {}", applicationId, finalVerdict.averageScore());
        }
    }

    private static String getSummaryPrompt(List<InterviewAnalysis> answers) {
        if (answers.isEmpty()) {
            throw new RuntimeException("No answers found to analyze.");
        }

        // B. Construct a log for the AI Recruiter
        StringBuilder interviewLog = new StringBuilder();
        int totalTech = 0;

        for (int i = 0; i < answers.size(); i++) {
            InterviewAnalysis a = answers.get(i);
            interviewLog.append(String.format("Q%d: %s\nA: %s\n[AI Stats: Tech=%d, Comm=%d, Generic=%s, Feedback=%s]\n\n",
                    i+1, a.getQuestion(), a.getCandidateAnswer(), a.getTechnicalScore(), a.getCommunicationScore(), a.isGeneric(), a.getAiFeedback()));

            totalTech += a.getTechnicalScore();
        }

        // C. Call AI for Final Verdict
        return PromptUtil.getRecruiterSummaryPrompt(interviewLog.toString());
    }

    @Override
    @Transactional
    public void updateApplicationStatus(Long applicationId, String status) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        try {
            // Convert string to Enum (e.g., "SHORTLISTED" -> ApplicationStatus.SHORTLISTED)
            ApplicationStatus newStatus = ApplicationStatus.valueOf(status.toUpperCase());
            app.setStatus(newStatus);

            // Optional: Update verdict if manually rejected
            if (newStatus == ApplicationStatus.REJECTED) {
                app.setInterviewVerdict(Verdict.REJECTED);
            } else if (newStatus == ApplicationStatus.SHORTLISTED) {
                app.setInterviewVerdict(Verdict.ACCEPTED);
            }

            jobApplicationRepository.save(app);
            log.info("Updated App ID {} status to {}", applicationId, newStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status provided: " + status);
        }
    }

    @Override
    @Transactional
    public void scheduleInterview(Long applicationId, ScheduleInterviewRequest request) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Create or Update the LiveInterview entity
        LiveInterview interview = liveInterviewRepository.findByApplicationId(applicationId)
                .orElse(LiveInterview.builder()
                        .application(app)
                        .status("SCHEDULED")
                        .build());

        interview.setScheduledAt(request.interviewDate());
        interview.setMeetingLink(request.interviewLink());
        interview.setStatus("SCHEDULED");

        liveInterviewRepository.save(interview);

        // Update main application status
        app.setStatus(ApplicationStatus.FINAL_INTERVIEW);
        jobApplicationRepository.save(app);
    }
}