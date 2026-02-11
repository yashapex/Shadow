package hiring_project.shadow_assistant.dto.auth;

public record AuthResponse(
        String token,
        UserProfileResponse userProfileresponse
) {}