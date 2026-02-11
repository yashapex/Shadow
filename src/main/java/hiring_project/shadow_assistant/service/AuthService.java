package hiring_project.shadow_assistant.service;

import hiring_project.shadow_assistant.dto.auth.LoginRequest;
import hiring_project.shadow_assistant.dto.auth.SignupRequest;
import hiring_project.shadow_assistant.dto.auth.TokenPair;

// Update the return type
public interface AuthService {
    TokenPair signup(SignupRequest request); // Change to TokenPair
    TokenPair login(LoginRequest request);   // Change to TokenPair
}