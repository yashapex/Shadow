package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.ai.EvaluationResponse;
import hiring_project.shadow_assistant.dto.ai.InterviewQuestionsResponse;
import hiring_project.shadow_assistant.entity.JobApplication;
import hiring_project.shadow_assistant.enums.ApplicationStatus;
import hiring_project.shadow_assistant.enums.Verdict;
import hiring_project.shadow_assistant.error.ResourceNotFoundException;
import hiring_project.shadow_assistant.repository.JobApplicationRepository;
import hiring_project.shadow_assistant.service.EliminatorService;
import hiring_project.shadow_assistant.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EliminatorServiceImpl implements EliminatorService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final JobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional
    public void evaluateCandidate(Long applicationId) {
        log.info("Starting AI Evaluation for Application ID: {}", applicationId);

        // 1. Fetch Application
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        Long candidateId = application.getCandidate().getId();
        Long jobId = application.getJob().getId();

        // 2. Fetch Content from Vector DB
        String jobContent = retrieveDocumentContent("JOB_" + jobId, "JD");
        if (jobContent.isEmpty()) {
            log.warn("No Job Description vectors found for Job ID {}", jobId);
            // Fallback: Use description from DB if vectors missing (Optional improvement)
            jobContent = application.getJob().getDescription();
        }

        String resumeContent = retrieveDocumentContent("CANDIDATE_" + candidateId, "RESUME");
        if (resumeContent.isEmpty()) {
            log.error("No Resume vectors found for Candidate ID {}", candidateId);
            throw new ResourceNotFoundException("Resume content missing for analysis.");
        }

        // 3. Generate Prompt
        String prompt = PromptUtil.getEliminatorPrompt(jobContent, resumeContent);

        // 4. Call AI
        EvaluationResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(EvaluationResponse.class);

        // 5. ⚡ AUTOMATION LOGIC
        int score = response.matchScore();
        application.setMatchScore(score);

        // Create detailed feedback
        String feedback = response.summary();
        if (response.missingSkills() != null && !response.missingSkills().isEmpty()) {
            feedback += "\n\nMissing Skills: " + String.join(", ", response.missingSkills());
        }
        application.setAiFeedback(feedback);

        // ⚡ AUTO-DECISION:
        if (score > 70) {
            application.setStatus(ApplicationStatus.SCREENING); // Auto-Promote
            application.setAiVerdict(Verdict.ACCEPTED);
        } else {
            application.setStatus(ApplicationStatus.REJECTED); // Auto-Reject
            application.setAiVerdict(Verdict.REJECTED);
        }

        // Save the changes
        jobApplicationRepository.save(application);
        log.info("AI Auto-Decision for App ID {}: Status={} Score={}", applicationId, application.getStatus(), score);
    }

    @Override
    public InterviewQuestionsResponse generateQuestions(Long candidateId, Long jobId) {
        String jobContent = retrieveDocumentContent("JOB_" + jobId, "JD");
        String resumeContent = retrieveDocumentContent("CANDIDATE_" + candidateId, "RESUME");

        if (jobContent.isEmpty() || resumeContent.isEmpty()) {
            throw new ResourceNotFoundException("Missing content for question generation.");
        }

        String prompt = PromptUtil.getQuestionGeneratorPrompt(jobContent, resumeContent);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(InterviewQuestionsResponse.class);
    }

    // --- Helper Methods ---

    private String retrieveDocumentContent(String referenceId, String docType) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            // ⚡ FIX: Use a broader query and higher topK to ensure we get all chunks
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("content") // Generic query to match everything filtered by ID
                            .topK(100)        // Increase limit to capture full resume
                            .similarityThreshold(0.0) // We want ALL chunks for this ID, not just "similar" ones
                            .filterExpression(
                                    b.and(
                                            b.eq("referenceId", referenceId),
                                            b.eq("docType", docType)
                                    ).build()
                            ).build()
            );

            return docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            log.error("Failed to retrieve documents for {}: {}", referenceId, e.getMessage());
            return "";
        }
    }
}