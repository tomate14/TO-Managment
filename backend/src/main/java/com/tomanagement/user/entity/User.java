package com.tomanagement.user.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A person who can sign in. Built with the Builder pattern; the email is always stored normalized.
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // Required by JPA.
    }

    private User(Builder builder) {
        this.email = EmailNormalizer.normalize(Objects.requireNonNull(builder.email, "email is required"));
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "passwordHash is required");
        this.fullName = Objects.requireNonNull(builder.fullName, "fullName is required");
        this.role = Objects.requireNonNull(builder.role, "role is required");
        this.active = builder.active;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Edits the profile fields an admin can change; the email is immutable.
     */
    public void updateProfile(String fullName, Role role) {
        this.fullName = Objects.requireNonNull(fullName, "fullName is required");
        this.role = Objects.requireNonNull(role, "role is required");
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public static final class Builder {

        private String email;
        private String passwordHash;
        private String fullName;
        private Role role;
        private boolean active = true;
        private Instant createdAt;

        private Builder() {
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
