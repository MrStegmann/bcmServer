package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.RegisterUserCommand;
import com.basketapp.bcmserver.application.port.out.PasswordEncoderPort;
import com.basketapp.bcmserver.application.port.out.UserRepositoryPort;
import com.basketapp.bcmserver.application.service.UserService;
import com.basketapp.bcmserver.domain.exception.UserAlreadyExistsException;
import com.basketapp.bcmserver.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void shouldRegisterUserSuccessfully() {
        RegisterUserCommand command = new RegisterUserCommand("new_user", "ValidPassword123!");
        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(passwordEncoder.encode("ValidPassword123!")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            return User.reconstitute(10L, userToSave.getUsername(), userToSave.getPasswordHash(), Instant.now());
        });

        User result = userService.register(command);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("new_user", result.getUsername());
        assertEquals("$2a$12$hashedPassword", result.getPasswordHash());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new_user", captor.getValue().getUsername());
        assertEquals("$2a$12$hashedPassword", captor.getValue().getPasswordHash());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username is taken")
    void shouldThrowWhenUsernameAlreadyExists() {
        RegisterUserCommand command = new RegisterUserCommand("existing_user", "ValidPassword123!");
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(command));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when password is blank")
    void shouldThrowWhenPasswordIsBlank() {
        RegisterUserCommand command = new RegisterUserCommand("valid_user", "   ");

        assertThrows(IllegalArgumentException.class, () -> userService.register(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SemanticValidationException when password lacks complexity")
    void shouldThrowWhenPasswordLacksComplexity() {
        RegisterUserCommand command = new RegisterUserCommand("valid_user", "weakpassword");

        assertThrows(com.basketapp.bcmserver.domain.exception.SemanticValidationException.class,
                () -> userService.register(command));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Should successfully retrieve user profile by ID")
    void shouldGetUserProfileByIdSuccessfully() {
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getProfileById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when ID does not exist")
    void shouldThrowWhenIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.basketapp.bcmserver.domain.exception.UserNotFoundException.class,
                () -> userService.getProfileById(999L));
    }

    @Test
    @DisplayName("Should successfully retrieve user profile by username")
    void shouldGetUserProfileByUsernameSuccessfully() {
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", Instant.now());
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        User result = userService.getProfileByUsername("john_doe");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when username does not exist")
    void shouldThrowWhenUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(com.basketapp.bcmserver.domain.exception.UserNotFoundException.class,
                () -> userService.getProfileByUsername("unknown"));
    }

    @Test
    @DisplayName("Should successfully update profile when requester is owner")
    void shouldUpdateProfileSuccessfullyWhenOwner() {
        com.basketapp.bcmserver.application.port.in.UpdateUserCommand command =
                new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(1L, 1L, "new_username", "NewSecret123!");
        User user = User.reconstitute(1L, "old_username", "$2a$12$oldhash", Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new_username")).thenReturn(false);
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("$2a$12$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = userService.updateProfile(command);

        assertEquals("new_username", updated.getUsername());
        assertEquals("$2a$12$newhash", updated.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw UnauthorizedOperationException when updating another user's profile")
    void shouldThrowWhenUpdatingOtherUserProfile() {
        com.basketapp.bcmserver.application.port.in.UpdateUserCommand command =
                new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(2L, 1L, "new_name", null);

        assertThrows(com.basketapp.bcmserver.domain.exception.UnauthorizedOperationException.class,
                () -> userService.updateProfile(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when updated username is taken")
    void shouldThrowWhenUpdatedUsernameIsTaken() {
        com.basketapp.bcmserver.application.port.in.UpdateUserCommand command =
                new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(1L, 1L, "taken_name", null);
        User user = User.reconstitute(1L, "current_name", "$2a$12$hash", Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken_name")).thenReturn(true);

        assertThrows(com.basketapp.bcmserver.domain.exception.UserAlreadyExistsException.class,
                () -> userService.updateProfile(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully delete user when requester is owner")
    void shouldDeleteUserSuccessfullyWhenOwner() {
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hash", Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L, 1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw UnauthorizedOperationException when deleting another user's account")
    void shouldThrowWhenDeletingOtherUser() {
        assertThrows(com.basketapp.bcmserver.domain.exception.UnauthorizedOperationException.class,
                () -> userService.deleteUser(2L, 1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when deleting non-existent account")
    void shouldThrowWhenDeletingNonExistentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.basketapp.bcmserver.domain.exception.UserNotFoundException.class,
                () -> userService.deleteUser(999L, 999L));
        verify(userRepository, never()).deleteById(any());
    }
}
