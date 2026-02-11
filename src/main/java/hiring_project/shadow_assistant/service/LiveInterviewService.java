package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.entity.LiveInterview;

public interface LiveInterviewService {
    LiveInterview getInterviewByApplicationId(Long applicationId);
    LiveInterview analyzeInterview(Long interviewId, String transcript);
}
