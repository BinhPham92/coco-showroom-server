package com.cocoshowroom.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// email and existsByEmail removed in W8 — identity lookups are now via UserIdentityRepository.
public interface UserRepository extends JpaRepository<User, UUID> {}
