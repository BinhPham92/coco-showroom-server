package com.cocoshowroom.server.auth;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link UserIdentity}: (provider, email).
 * Required by JPA's {@code @IdClass} mechanism.
 */
public class UserIdentityId implements Serializable {

    private OAuthProvider provider;
    private String email;

    public UserIdentityId() {}

    public UserIdentityId(OAuthProvider provider, String email) {
        this.provider = provider;
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserIdentityId that)) return false;
        return provider == that.provider && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, email);
    }
}
