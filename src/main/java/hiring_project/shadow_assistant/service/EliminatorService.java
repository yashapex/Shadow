package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;

public interface EliminatorService {
    // ⚡ UPDATED: Returns void because it saves to DB directly
    void evaluateCandidate(Long applicationId);

    InterviewQuestionsResponse generateQuestions(Long candidateId, Long jobId);
}