package hiring_project.shadow_assistant.dto.ai;

public record AudioAnalysisResponse(
        int technicalScore,      // 0-100: Do they actually know the concept?
        int communicationScore,  // 0-100: How confident/clear are they?
        boolean isGeneric,       // True if it sounds like a Wikipedia definition
        String analysis          // Short feedback for the recruiter
) {}