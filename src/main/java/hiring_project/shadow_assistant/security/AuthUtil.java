package hiring_project.shadow_assistant.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import hiring_project.shadow_assistant.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;

@Component
public class AuthUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("type", "ACCESS") // ⚡ Security check
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 Minutes
                .signWith(getSignInKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId()) // Need ID to regenerate access token later
                .claim("type", "REFRESH") // ⚡ Security check
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24 * 30)) // 30 Days
                .signWith(getSignInKey())
                .compact();
    }

    public JwtUserPrincipal verifyAccessToken(String token) {
        return verifyToken(token, "ACCESS");
    }

    public JwtUserPrincipal verifyRefreshToken(String token) {
        return verifyToken(token, "REFRESH");
    }

    private JwtUserPrincipal verifyToken(String token, String requiredType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // ⚡ Security Check: Ensure token is the correct type
            String type = claims.get("type", String.class);
            if (!requiredType.equals(type)) {
                return null; // Reject if trying to use Access Token as Refresh Token
            }

            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();

            return new JwtUserPrincipal(userId, username, new ArrayList<>());
        } catch (Exception e) {
            return null; // Token invalid or expired
        }
    }

    // ... existing code ...

    public JwtUserPrincipal getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal user) {
            return user;
        }
        throw new RuntimeException("No user logged in!");
    }

    public Long getLoggedInUserId() {
        return getLoggedInUser().userId();
    }

    public String getLoggedInUserRole() {
        // This assumes you added the role to the token claims or principal earlier
        // If not, we might need to fetch it or trust the token's claim
        return getLoggedInUser().authorities().toString();
    }
}