package hiring_project.shadow_assistant.dto.ai;

import java.util.List;

public record EvaluationResponse(
        int matchScore,           // 0-100
        String summary,           // Short explanation
        List<String> missingSkills, // What the candidate lacks
        String verdict            // "STRONG MATCH", "MODERATE", or "REJECT"
) {}