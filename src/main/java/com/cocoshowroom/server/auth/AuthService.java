package com.cocoshowroom.server.auth;

import com.cocoshowroom.server.auth.social.SocialIdentity;
import com.cocoshowroom.server.auth.social.SocialVerifierFactory;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final JwtService jwtService;
    private final SocialVerifierFactory verifierFactory;
    private final TransactionTemplate txTemplate;

    /**
     * Authenticates (or registers) a user via social OAuth.
     *
     * <ol>
     *   <li>Verifies the provider token and extracts a {@link SocialIdentity}.</li>
     *   <li>Looks up an existing {@link UserIdentity} by (provider, email).</li>
     *   <li>If not found, creates a new {@link User} and {@link UserIdentity} in a
     *       managed transaction — this is the implicit registration path.</li>
     *   <li>Issues a signed JWT and returns {@link SocialAuthResponse}.</li>
     * </ol>
     *
     * <p><b>Concurrency:</b> two simultaneous first-logins for the same email will
     * race at the (provider, email) unique index. The losing transaction is rolled
     * back and throws {@link DataIntegrityViolationException}; this method catches
     * that exception and re-reads the identity the winning transaction wrote,
     * returning a valid login response instead of a 500.
     */
    public SocialAuthResponse socialSignIn(SocialAuthRequest request) {
        // Token verification happens outside any transaction — it involves a remote
        // HTTP call (JWKS/Graph API) that must not hold a DB connection.
        SocialIdentity identity = verifierFactory.get(request.provider()).verify(request.token());

        try {
            return txTemplate.execute(status -> doSignIn(request.provider(), identity));
        } catch (DataIntegrityViolationException ex) {
            // Race loser: the winning concurrent first-login committed the identity row.
            // Open a new read-only transaction to fetch the row the winner created.
            return txTemplate.execute(status -> {
                UserIdentity existing = userIdentityRepository
                    .findByProviderAndEmail(request.provider(), identity.email())
                    .orElseThrow(() -> new IllegalStateException(
                        "Expected identity after concurrent sign-in race for email: "
                        + identity.email()));
                User user = existing.getUser();
                String token = jwtService.generateToken(user, identity.email());
                return new SocialAuthResponse(
                    token, user.getId(), identity.email(), user.getRole().name(), false);
            });
        }
    }

    /** Core sign-in logic executed inside a transaction managed by {@link #txTemplate}. */
    private SocialAuthResponse doSignIn(OAuthProvider provider, SocialIdentity identity) {
        UserIdentity userIdentity = userIdentityRepository
            .findByProviderAndEmail(provider, identity.email())
            .orElse(null);

        boolean isNewUser = false;

        if (userIdentity == null) {
            // First time this provider+email combination is seen — create account.
            User user = new User();
            user.setName(identity.name());
            user.setRole(Role.CUSTOMER);
            user = userRepository.save(user);

            userIdentity = new UserIdentity();
            userIdentity.setProvider(provider);
            userIdentity.setEmail(identity.email());
            userIdentity.setUser(user);
            userIdentity.setProviderSubject(identity.providerSubject());
            userIdentityRepository.save(userIdentity);

            isNewUser = true;
        }

        User user = userIdentity.getUser();
        String token = jwtService.generateToken(user, identity.email());
        return new SocialAuthResponse(token, user.getId(), identity.email(), user.getRole().name(), isNewUser);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.name() != null) {
            user.setName(request.name().isBlank() ? null : request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isEmpty() ? null : request.phone());
        }
        user = userRepository.save(user);

        // Read email from the identity (oldest one wins if multiple exist post-v1)
        String email = userIdentityRepository.findFirstByUserOrderByCreatedAtAsc(user)
            .map(UserIdentity::getEmail)
            .orElse("");
        return UserResponse.from(user, email);
    }
}
