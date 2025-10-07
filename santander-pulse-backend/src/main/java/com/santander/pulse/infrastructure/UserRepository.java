package com.santander.pulse.infrastructure;

import com.santander.pulse.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides banking-specific query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by CPF
     */
    Optional<User> findByCpf(String cpf);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if CPF exists
     */
    boolean existsByCpf(String cpf);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find users by role
     */
    List<User> findByRole(User.Role role);

    /**
     * Find users created after specific date
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Find users with specific roles
     */
    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.enabled = true")
    List<User> findEnabledUsersByRoles(@Param("roles") List<User.Role> roles);

    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();
}