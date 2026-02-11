package hiring_project.shadow_assistant.dto.ai;

import java.util.List;

public record InterviewSummaryRequest(
        Long candidateId,
        List<QuestionResult> results
) {
    // Nested record to define the structure of each question's result
    public record QuestionResult(
            String question,
            String transcript,
            int score,
            boolean isGeneric
    ) {}
}