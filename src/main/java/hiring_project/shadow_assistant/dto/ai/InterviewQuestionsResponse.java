package hiring_project.shadow_assistant.dto.ai;

import java.util.List;

public record InterviewQuestionsResponse(
        List<String> questions,
        String focusArea // e.g., "Backend System Design" or "Java Core"
) {}