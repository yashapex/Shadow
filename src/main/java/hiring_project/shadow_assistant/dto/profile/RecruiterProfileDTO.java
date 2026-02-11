package hiring_project.shadow_assistant.dto.profile;

public record RecruiterProfileDTO(
        Long id,
        String fullName,      // From User entity
        String email,         // From User entity
        String position,      // "Role" in UI
        String department,
        String companyName,
        String location,
        String avatarUrl
) {}