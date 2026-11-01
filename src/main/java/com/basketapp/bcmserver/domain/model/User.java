package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Pure Java 21 User domain entity.
 * Free from framework and persistence annotations.
 */
public class User {

    private final Long id;
    private String username;
    private String passwordHash;
    private UserStatus status;
    private final Instant createdAt;

    private User(Long id, String username, String passwordHash, UserStatus status, Instant createdAt) {
        validateUsername(username);
        validatePasswordHash(passwordHash);

        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static User create(String username, String passwordHash) {
        return new User(null, username, passwordHash, UserStatus.ACTIVE, Instant.now());
    }

    public static User reconstitute(Long id, String username, String passwordHash, Instant createdAt) {
        return reconstitute(id, username, passwordHash, UserStatus.ACTIVE, createdAt);
    }

    public static User reconstitute(Long id, String username, String passwordHash, UserStatus status, Instant createdAt) {
        Objects.requireNonNull(id, "ID cannot be null when reconstituting existing user");
        Objects.requireNonNull(createdAt, "createdAt cannot be null when reconstituting existing user");
        return new User(id, username, passwordHash, status, createdAt);
    }

    public boolean isEligibleForLogin() {
        return this.status == UserStatus.ACTIVE;
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void updateUsername(String newUsername) {
        validateUsername(newUsername);
        this.username = newUsername;
    }

    public void updatePasswordHash(String newPasswordHash) {
        validatePasswordHash(newPasswordHash);
        this.passwordHash = newPasswordHash;
    }

    public boolean hasId(Long id) {
        return this.id != null && this.id.equals(id);
    }

    private static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            throw new IllegalArgumentException("Username length must be between 3 and 50 characters");
        }
    }

    private static void validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
