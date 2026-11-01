package com.basketapp.bcmserver.domain;

import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.domain.model.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("Should create valid new User with null id, generated createdAt, and default ACTIVE status")
    void shouldCreateValidNewUser() {
        User user = User.create("valid_user", "$2a$12$e/sampleHashedPassword123");

        assertNull(user.getId());
        assertEquals("valid_user", user.getUsername());
        assertEquals("$2a$12$e/sampleHashedPassword123", user.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isEligibleForLogin());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Should reconstitute existing User with all persistent fields including status")
    void shouldReconstituteExistingUser() {
        Instant now = Instant.now();
        User user = User.reconstitute(1L, "existing_user", "$2a$12$hashed", UserStatus.LOCKED, now);

        assertEquals(1L, user.getId());
        assertEquals("existing_user", user.getUsername());
        assertEquals("$2a$12$hashed", user.getPasswordHash());
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertFalse(user.isEligibleForLogin());
        assertEquals(now, user.getCreatedAt());
        assertTrue(user.hasId(1L));
        assertFalse(user.hasId(2L));
    }

    @Test
    @DisplayName("Should update status via lifecycle methods")
    void shouldUpdateStatusLifecycle() {
        User user = User.create("valid_user", "$2a$12$e/sampleHashedPassword123");
        assertTrue(user.isEligibleForLogin());

        user.lock();
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertFalse(user.isEligibleForLogin());

        user.activate();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isEligibleForLogin());

        user.deactivate();
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertFalse(user.isEligibleForLogin());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("Should reject invalid username on create")
    void shouldRejectInvalidUsername(String invalidUsername) {
        assertThrows(IllegalArgumentException.class, () ->
                User.create(invalidUsername, "$2a$12$hashed")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Should reject invalid password hash on create")
    void shouldRejectInvalidPasswordHash(String invalidHash) {
        assertThrows(IllegalArgumentException.class, () ->
                User.create("valid_user", invalidHash)
        );
    }

    @Test
    @DisplayName("Should update username with valid new value")
    void shouldUpdateUsername() {
        User user = User.create("old_name", "$2a$12$hashed");
        user.updateUsername("new_name");
        assertEquals("new_name", user.getUsername());
    }

    @Test
    @DisplayName("Should update password hash with valid new hash")
    void shouldUpdatePasswordHash() {
        User user = User.create("valid_user", "$2a$12$oldhash");
        user.updatePasswordHash("$2a$12$newhash");
        assertEquals("$2a$12$newhash", user.getPasswordHash());
    }
}
