package com.tomanagement.user.repository;

import java.util.Optional;

import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Case-insensitive lookup (matches the {@code lower(email)} unique index), so users inserted
     * by hand with a mixed-case email can still be found.
     *
     * @param email the email as typed; callers normally pass it through EmailNormalizer first
     */
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    boolean existsByRole(Role role);
}
