package com.cocoshowroom.server.auth;

import com.cocoshowroom.server.shared.InvalidCredentialsException;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Changes the authenticated user's password after verifying the current one.
     *
     * <p>Throws {@link InvalidCredentialsException} (401) if the current password
     * is wrong — same error as a bad sign-in so callers can't distinguish
     * "wrong password" from "user not found".
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public AuthResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }
}
