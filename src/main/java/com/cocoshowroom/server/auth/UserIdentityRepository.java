package com.cocoshowroom.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UserIdentityId> {

    Optional<UserIdentity> findByProviderAndEmail(OAuthProvider provider, String email);

    boolean existsByProviderAndEmail(OAuthProvider provider, String email);

    /** Returns any identity linked to this user (used to surface email on GET /me). */
    Optional<UserIdentity> findFirstByUserOrderByCreatedAtAsc(User user);
}
