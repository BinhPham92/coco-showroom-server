package com.cocoshowroom.server.auth;

import com.cocoshowroom.server.shared.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /** Issues a signed JWT for valid credentials. */
    @PostMapping("/sign-in")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest request) {
        return authService.signIn(request);
    }

    /**
     * Stateless sign-out — client must discard the token.
     * No server-side blacklist until a Redis store is added (post-v1).
     */
    @PostMapping("/sign-out")
    public void signOut() {
        // Intentionally empty: client clears the stored token on 200 OK.
    }

    /** Returns the authenticated user's profile. Requires a valid JWT. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.from(user);
    }
}
