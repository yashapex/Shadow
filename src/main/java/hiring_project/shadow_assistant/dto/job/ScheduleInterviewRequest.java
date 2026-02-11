package hiring_project.shadow_assistant.dto.job;

import java.time.LocalDateTime;

public record ScheduleInterviewRequest(
        LocalDateTime interviewDate,
        String interviewLink
) {}