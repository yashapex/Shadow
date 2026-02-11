package hiring_project.shadow_assistant.dto.ai;

public record InterviewAnalysisResponse(
        int technicalScore,
        int communicationScore,
        boolean isGeneric,
        String analysis
) {}