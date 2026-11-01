package com.basketapp.bcmserver.application;

import com.basketapp.bcmserver.application.port.in.AuthToken;
import com.basketapp.bcmserver.application.port.in.AuthenticationResult;
import com.basketapp.bcmserver.application.port.in.LoginUserCommand;
import com.basketapp.bcmserver.application.port.out.PasswordEncoderPort;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.application.port.out.UserRepositoryPort;
import com.basketapp.bcmserver.application.service.AuthService;
import com.basketapp.bcmserver.domain.exception.AccountStatusException;
import com.basketapp.bcmserver.domain.exception.InvalidCredentialsException;
import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenProviderPort tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("Should authenticate user and issue JWT token when credentials match")
    void shouldAuthenticateSuccessfully() {
        LoginUserCommand command = new LoginUserCommand("john_doe", "CorrectPassword123!");
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", UserStatus.ACTIVE, Instant.now());
        AuthToken authToken = AuthToken.bearer("sample-jwt-token", 3600);

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$12$hashed")).thenReturn(true);
        when(tokenProvider.generateToken(user)).thenReturn(authToken);

        AuthenticationResult result = authService.login(command);

        assertNotNull(result);
        assertEquals(user, result.user());
        assertEquals("sample-jwt-token", result.token().token());
        assertEquals("Bearer", result.token().tokenType());
        assertEquals(3600, result.token().expiresIn());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException and perform dummy password check when username does not exist")
    void shouldThrowWhenUsernameNotFoundWithAntiEnumeration() {
        LoginUserCommand command = new LoginUserCommand("unknown_user", "AnyPassword123!");
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(command));
        verify(passwordEncoder).matches(eq("AnyPassword123!"), any());
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match hash")
    void shouldThrowWhenPasswordMismatch() {
        LoginUserCommand command = new LoginUserCommand("john_doe", "WrongPassword123!");
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", UserStatus.ACTIVE, Instant.now());

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", "$2a$12$hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(command));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("Should throw AccountStatusException (403) when user status is LOCKED")
    void shouldThrowWhenUserIsLocked() {
        LoginUserCommand command = new LoginUserCommand("locked_user", "CorrectPassword123!");
        User user = User.reconstitute(1L, "locked_user", "$2a$12$hashed", UserStatus.LOCKED, Instant.now());

        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$12$hashed")).thenReturn(true);

        assertThrows(AccountStatusException.class, () -> authService.login(command));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("Should throw AccountStatusException (403) when user status is INACTIVE")
    void shouldThrowWhenUserIsInactive() {
        LoginUserCommand command = new LoginUserCommand("inactive_user", "CorrectPassword123!");
        User user = User.reconstitute(1L, "inactive_user", "$2a$12$hashed", UserStatus.INACTIVE, Instant.now());

        when(userRepository.findByUsername("inactive_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$12$hashed")).thenReturn(true);

        assertThrows(AccountStatusException.class, () -> authService.login(command));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("Should propagate RuntimeException when token provider fails")
    void shouldThrowWhenTokenProviderFails() {
        LoginUserCommand command = new LoginUserCommand("john_doe", "CorrectPassword123!");
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", UserStatus.ACTIVE, Instant.now());

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "$2a$12$hashed")).thenReturn(true);
        when(tokenProvider.generateToken(user)).thenThrow(new RuntimeException("Cryptographic signing error"));

        assertThrows(RuntimeException.class, () -> authService.login(command));
    }
}
