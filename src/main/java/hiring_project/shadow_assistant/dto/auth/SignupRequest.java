package hiring_project.shadow_assistant.dto.auth;

import hiring_project.shadow_assistant.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Username (username) is required")
        @Email(message = "Please provide a valid username address")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "Name is required")
        String name,

        Role role,       // RECRUITER or CANDIDATE
        String adminKey  // Optional
) {}