package hiring_project.shadow_assistant.dto.auth;

public record TokenPair(
        String accessToken,
        String refreshToken,
        UserProfileResponse userProfile
) {}