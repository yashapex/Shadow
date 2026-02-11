package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;
import hiring_project.shadow_assistant.dto.job.ScheduleInterviewRequest;
import hiring_project.shadow_assistant.entity.JobApplication;
import java.util.List;
import java.util.Map;

public interface JobApplicationService {
    void applyForJob(Long jobId);
    List<JobApplication> getMyApplications();

    // Recruiter Methods
    List<JobApplication> getApplicationsForJob(Long jobId);
    List<JobApplication> getApplicationsForRecruiter();
    JobApplication getApplicationById(Long applicationId);
    JobApplication downloadResume(Long applicationId);

    // ⚡ FIX: Return single object containing the list
    InterviewQuestionsResponse generatePreInterviewQuestions(Long applicationId);

    void submitInterview(Long applicationId, Map<String, Object> results);
    void submitAnswer(Long applicationId, String question, String answer);
    void finalizeInterview(Long applicationId);
    void updateApplicationStatus(Long applicationId, String status);
    void scheduleInterview(Long applicationId, ScheduleInterviewRequest request);
}