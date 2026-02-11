package hiring_project.shadow_assistant.service.impl;

import hiring_project.shadow_assistant.dto.auth.*;
import hiring_project.shadow_assistant.entity.User;
import hiring_project.shadow_assistant.enums.Role;
import hiring_project.shadow_assistant.repository.UserRepository;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${app.recruiter-secret}")
    private String recruiterSecret;

    public TokenPair signup(SignupRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Error: User with this username already exists!");
        }
        // 1. Recruiter Check
        if (request.role() == Role.RECRUITER) {
            if (!recruiterSecret.equals(request.adminKey())) {
                throw new RuntimeException("Invalid Admin Key!");
            }
        }

        // 2. Save User
        var user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(request.role())
                .build();

        userRepository.save(user);

        // Generate Tokens
        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        return buildTokenPair(user, accessToken, refreshToken);
    }

    public TokenPair login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = authUtil.generateAccessToken(user);
        String refreshToken = authUtil.generateRefreshToken(user);

        return buildTokenPair(user, accessToken, refreshToken);
    }

    // --- Helper Method to keep code clean ---

    private TokenPair buildTokenPair(User user, String accessToken, String refreshToken) {
        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole()
        );

        return new TokenPair(accessToken, refreshToken, profile);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole()
        );

        return new AuthResponse(token, profile);
    }
}