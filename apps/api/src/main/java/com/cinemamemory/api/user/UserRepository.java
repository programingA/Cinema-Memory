package com.cinemamemory.api.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    List<User> findAllByOrderByCreatedAtDesc();
}
