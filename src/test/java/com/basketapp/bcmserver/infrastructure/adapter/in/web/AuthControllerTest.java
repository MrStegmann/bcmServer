package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.LoginUserCommand;
import com.basketapp.bcmserver.application.port.in.LoginUserUseCase;
import com.basketapp.bcmserver.application.port.in.RegisterUserCommand;
import com.basketapp.bcmserver.application.port.in.RegisterUserUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.AccountStatusException;
import com.basketapp.bcmserver.domain.exception.InvalidCredentialsException;
import com.basketapp.bcmserver.domain.exception.RateLimitExceededException;
import com.basketapp.bcmserver.domain.exception.SemanticValidationException;
import com.basketapp.bcmserver.domain.exception.UserAlreadyExistsException;
import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.domain.model.UserStatus;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.AuthResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.LoginRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UserProfileResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.UserWebMapper;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private UserWebMapper userWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    // --- Register Tests (US1) ---

    @Test
    @DisplayName("Should return 201 Created and user profile omitting password on valid registration")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("john_doe", "SecurePassword123!");
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", UserStatus.ACTIVE, Instant.now());
        UserProfileResponse response = new UserProfileResponse(1L, "john_doe", "ACTIVE", user.getCreatedAt());

        when(userWebMapper.toCommand(any(RegisterRequest.class)))
                .thenReturn(new RegisterUserCommand("john_doe", "SecurePassword123!"));
        when(registerUserUseCase.register(any(RegisterUserCommand.class))).thenReturn(user);
        when(userWebMapper.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return 400 Bad Request with ProblemDetails when validation fails on registration")
    void shouldReturn400WhenValidationFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("a", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.username").exists())
                .andExpect(jsonPath("$.invalidParams.password").exists());
    }

    @Test
    @DisplayName("Should return 409 Conflict when username already exists")
    void shouldReturn409WhenUsernameExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existing_user", "SecurePassword123!");

        when(userWebMapper.toCommand(any(RegisterRequest.class)))
                .thenReturn(new RegisterUserCommand("existing_user", "SecurePassword123!"));
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new UserAlreadyExistsException("existing_user"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 422 Unprocessable Entity when semantic password complexity validation fails")
    void shouldReturn422WhenPasswordComplexityFails() throws Exception {
        RegisterRequest request = new RegisterRequest("weak_user", "weakpassword");

        when(userWebMapper.toCommand(any(RegisterRequest.class)))
                .thenReturn(new RegisterUserCommand("weak_user", "weakpassword"));
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new SemanticValidationException(
                        "Password does not meet minimum security requirements",
                        Map.of("password", "Password is too weak")
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Password does not meet minimum security requirements"))
                .andExpect(jsonPath("$.invalidParams.password").value("Password is too weak"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when registration rate limit is exceeded")
    void shouldReturn429WhenRegistrationRateLimitExceeded() throws Exception {
        RegisterRequest request = new RegisterRequest("valid_user", "SecurePassword123!");

        when(userWebMapper.toCommand(any(RegisterRequest.class)))
                .thenReturn(new RegisterUserCommand("valid_user", "SecurePassword123!"));
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded. Please try again later.", 60));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when unexpected exception occurs on registration")
    void shouldReturn500WhenRegistrationFailsUnexpectedly() throws Exception {
        RegisterRequest request = new RegisterRequest("valid_user", "SecurePassword123!");

        when(userWebMapper.toCommand(any(RegisterRequest.class)))
                .thenReturn(new RegisterUserCommand("valid_user", "SecurePassword123!"));
        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new RuntimeException("Database connection timeout"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    // --- Login Tests (US2) ---

    @Test
    @DisplayName("Should return 200 OK and JWT token on successful login")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("john_doe", "CorrectPassword123!");
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", UserStatus.ACTIVE, Instant.now());
        UserProfileResponse userResponse = new UserProfileResponse(1L, "john_doe", "ACTIVE", user.getCreatedAt());
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "Bearer", 3600, userResponse);

        when(userWebMapper.toCommand(any(LoginRequest.class)))
                .thenReturn(new LoginUserCommand("john_doe", "CorrectPassword123!"));
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenReturn(new com.basketapp.bcmserver.application.port.in.AuthenticationResult(
                        com.basketapp.bcmserver.application.port.in.AuthToken.bearer("mock.jwt.token", 3600),
                        user
                ));
        when(userWebMapper.toAuthResponse(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.username").value("john_doe"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when login request payload is invalid")
    void shouldReturn400WhenLoginPayloadInvalid() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.username").exists())
                .andExpect(jsonPath("$.invalidParams.password").exists());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when credentials are invalid")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("john_doe", "WrongPassword123!");

        when(userWebMapper.toCommand(any(LoginRequest.class)))
                .thenReturn(new LoginUserCommand("john_doe", "WrongPassword123!"));
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when account status is locked or inactive")
    void shouldReturn403WhenAccountIsLockedOrInactive() throws Exception {
        LoginRequest request = new LoginRequest("locked_user", "CorrectPassword123!");

        when(userWebMapper.toCommand(any(LoginRequest.class)))
                .thenReturn(new LoginUserCommand("locked_user", "CorrectPassword123!"));
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new AccountStatusException("Account is locked or inactive"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Account is locked or inactive"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when login rate limit is exceeded")
    void shouldReturn429WhenLoginRateLimitExceeded() throws Exception {
        LoginRequest request = new LoginRequest("john_doe", "CorrectPassword123!");

        when(userWebMapper.toCommand(any(LoginRequest.class)))
                .thenReturn(new LoginUserCommand("john_doe", "CorrectPassword123!"));
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded. Please try again later.", 60));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when unexpected error occurs during login")
    void shouldReturn500WhenLoginFailsUnexpectedly() throws Exception {
        LoginRequest request = new LoginRequest("john_doe", "CorrectPassword123!");

        when(userWebMapper.toCommand(any(LoginRequest.class)))
                .thenReturn(new LoginUserCommand("john_doe", "CorrectPassword123!"));
        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new RuntimeException("Cryptographic token signing failure"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
