package com.cocoshowroom.server.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Links a social identity (provider + email) to a {@link User} account.
 *
 * <p>Primary key is the composite ({@code provider}, {@code email}) pair — the
 * same email address on two different providers produces two separate rows and
 * two separate user accounts (no automatic cross-provider linking in v1).
 *
 * <p>A single user may accumulate multiple identity rows in future (post-v1
 * account-linking feature), hence the {@code ManyToOne} relationship.
 */
@Entity
@Table(name = "user_identities")
@IdClass(UserIdentityId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserIdentity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Id
    @Column(nullable = false, length = 320)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)   // mirrors ON DELETE CASCADE in V9 migration
    private User user;

    /**
     * The provider's own stable user identifier (Google {@code sub}, Facebook {@code id}).
     * Stored for audit purposes; not used for lookups in v1.
     */
    @Column(length = 255)
    private String providerSubject;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
