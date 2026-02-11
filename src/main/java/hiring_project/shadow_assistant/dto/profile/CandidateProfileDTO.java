package hiring_project.shadow_assistant.dto.profile;

import java.time.LocalDateTime;

public record CandidateProfileDTO(
        Long userId,
        String name,             // Maps to 'fullName' in frontend
        String username,         // Maps to 'email' in frontend
        String jobTitle,
        String phone,
        String location,
        String bio,
        String skills,        // Comma-separated string
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        String avatarUrl,
        LocalDateTime resumeLastUpdated
) {

}