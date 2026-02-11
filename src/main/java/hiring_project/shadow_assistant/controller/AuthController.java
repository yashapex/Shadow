package hiring_project.shadow_assistant.controller;

import hiring_project.shadow_assistant.dto.auth.AuthResponse;
import hiring_project.shadow_assistant.dto.auth.LoginRequest;
import hiring_project.shadow_assistant.dto.auth.SignupRequest;
import hiring_project.shadow_assistant.dto.auth.TokenPair;
import hiring_project.shadow_assistant.entity.User;
import hiring_project.shadow_assistant.repository.UserRepository;
import hiring_project.shadow_assistant.security.AuthUtil;
import hiring_project.shadow_assistant.security.JwtUserPrincipal;
import hiring_project.shadow_assistant.service.AuthService;
import hiring_project.shadow_assistant.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request) {
        TokenPair tokenPair = authService.signup(request);
        return sendTokenResponse(tokenPair);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        TokenPair tokenPair = authService.login(request);
        return sendTokenResponse(tokenPair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(403).build();
        }

        // 1. Verify the Refresh Token
        JwtUserPrincipal principal = authUtil.verifyRefreshToken(refreshToken);
        if (principal == null) {
            return ResponseEntity.status(403).build(); // Invalid or Expired
        }

        // 2. Fetch User (to ensure they weren't deleted/banned in the meantime)
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate NEW Access Token
        String newAccessToken = authUtil.generateAccessToken(user);

        // 4. Return new Access Token (Profile can be null as frontend already has it)
        return ResponseEntity.ok(new AuthResponse(newAccessToken, null));
    }


    // --- Helper to set Cookie & Body ---
    private ResponseEntity<AuthResponse> sendTokenResponse(TokenPair tokens) {
        // Create the HttpOnly Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)       // Frontend JS cannot read this
                .secure(false)        // Set TRUE if using HTTPS in production
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 Days
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(tokens.accessToken(), tokens.userProfile()));
    }
}