package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.job.JobPostRequest;
import hiring_project.shadow_assistant.entity.Job;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface JobService {
    Job createJob(JobPostRequest request, MultipartFile file) throws IOException;
    List<Job> getMyJobs();
    List<Job> getAllJobs(); // <--- ⚡ NEW: For Candidates
    void deleteJob(Long jobId);
    Job updateJob(Long jobId, JobPostRequest request);
    Job getJobById(Long jobId);
}
