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

    /**
     * Signs in (or registers) a user via a social OAuth token.
     * Returns a signed JWT and a flag indicating whether this is a new account.
     */
    @PostMapping("/social")
    public SocialAuthResponse social(@Valid @RequestBody SocialAuthRequest request) {
        return authService.socialSignIn(request);
    }

    /**
     * Stateless sign-out — client must discard the token.
     * No server-side blacklist until a Redis store is added (post-v1).
     */
    @PostMapping("/sign-out")
    public void signOut() {
        // Intentionally empty: the BFF clears the HttpOnly session cookie on 200 OK.
    }

    /**
     * Returns the authenticated user's profile.
     * Email is read from the JWT claim (stored there at sign-in time) so no
     * extra UserIdentity query is needed on every profile fetch.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.from(user, email);
    }

    /**
     * Updates the authenticated user's display name and/or phone.
     * Only non-null fields in the body are applied.
     */
    @PatchMapping("/me")
    public UserResponse updateProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return authService.updateProfile(userId, request);
    }
}
