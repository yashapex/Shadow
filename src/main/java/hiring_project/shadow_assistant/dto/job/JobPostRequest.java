package hiring_project.shadow_assistant.dto.job;

import lombok.Data;

public record JobPostRequest(
         String title,
         String location,
         String salaryRange,
         String experienceLevel,
         String requiredSkills,
         String interviewType,
         String description // The text typed in the text area
) { }