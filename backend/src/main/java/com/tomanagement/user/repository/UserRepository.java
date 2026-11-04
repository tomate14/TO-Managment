package com.tomanagement.user.repository;

import java.util.Optional;

import com.tomanagement.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * @param normalizedEmail an email already passed through EmailNormalizer
     */
    Optional<User> findByEmail(String normalizedEmail);
}
