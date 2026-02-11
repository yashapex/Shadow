package hiring_project.shadow_assistant.dto.auth;

import hiring_project.shadow_assistant.enums.Role;

public record UserProfileResponse(
        Long id,
        String name,
        String username,
        Role role // <--- ⚠️ Critical for Frontend Redirection
) {
}
