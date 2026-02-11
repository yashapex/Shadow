package hiring_project.shadow_assistant.security;

import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public record JwtUserPrincipal(
        Long userId,
        String username,
        Collection<? extends GrantedAuthority> authorities
) {}