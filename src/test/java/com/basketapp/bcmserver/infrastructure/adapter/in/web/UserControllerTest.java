package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.DeleteUserUseCase;
import com.basketapp.bcmserver.application.port.in.GetUserProfileUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateUserProfileUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.UnauthorizedOperationException;
import com.basketapp.bcmserver.domain.exception.UserAlreadyExistsException;
import com.basketapp.bcmserver.domain.exception.UserNotFoundException;
import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UserProfileResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.UserWebMapper;
import com.basketapp.bcmserver.infrastructure.config.AuthenticatedUser;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetUserProfileUseCase getUserProfileUseCase;

    @MockBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockBean
    private UserWebMapper userWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    private UsernamePasswordAuthenticationToken mockAuth(Long userId, String username) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should return 200 OK and current user profile for GET /api/v1/users/me")
    void shouldGetCurrentUserProfile() throws Exception {
        User user = User.reconstitute(1L, "john_doe", "$2a$12$hashed", Instant.now());
        UserProfileResponse response = new UserProfileResponse(1L, "john_doe", user.getCreatedAt());

        when(getUserProfileUseCase.getProfileById(1L)).thenReturn(user);
        when(userWebMapper.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Should return 200 OK and user profile for GET /api/v1/users/{id}")
    void shouldGetUserProfileById() throws Exception {
        User user = User.reconstitute(2L, "jane_doe", "$2a$12$hashed", Instant.now());
        UserProfileResponse response = new UserProfileResponse(2L, "jane_doe", user.getCreatedAt());

        when(getUserProfileUseCase.getProfileById(2L)).thenReturn(user);
        when(userWebMapper.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/2")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.username").value("jane_doe"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when user ID does not exist")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(getUserProfileUseCase.getProfileById(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/v1/users/999")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should return 200 OK and updated user profile for PUT /api/v1/users/{id} when requester is owner")
    void shouldUpdateUserSuccessfully() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("new_username", "NewSecret123!");
        User user = User.reconstitute(1L, "new_username", "$2a$12$newhash", Instant.now());
        UserProfileResponse response = new UserProfileResponse(1L, "new_username", user.getCreatedAt());

        when(userWebMapper.toCommand(eq(1L), eq(1L), any(UpdateUserRequest.class)))
                .thenReturn(new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(1L, 1L, "new_username", "NewSecret123!"));
        when(updateUserProfileUseCase.updateProfile(any())).thenReturn(user);
        when(userWebMapper.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(authentication(mockAuth(1L, "old_username")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("new_username"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when updating another user's profile")
    void shouldReturn403WhenUpdatingOtherUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("new_username", null);

        when(userWebMapper.toCommand(eq(2L), eq(1L), any(UpdateUserRequest.class)))
                .thenReturn(new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(2L, 1L, "new_username", null));
        when(updateUserProfileUseCase.updateProfile(any()))
                .thenThrow(new UnauthorizedOperationException("You do not have permission to update account ID 2"));

        mockMvc.perform(put("/api/v1/users/2")
                        .with(authentication(mockAuth(1L, "john_doe")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Should return 409 Conflict when updated username is already taken")
    void shouldReturn409WhenUpdatingWithDuplicateUsername() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("taken_username", null);

        when(userWebMapper.toCommand(eq(1L), eq(1L), any(UpdateUserRequest.class)))
                .thenReturn(new com.basketapp.bcmserver.application.port.in.UpdateUserCommand(1L, 1L, "taken_username", null));
        when(updateUserProfileUseCase.updateProfile(any()))
                .thenThrow(new UserAlreadyExistsException("taken_username"));

        mockMvc.perform(put("/api/v1/users/1")
                        .with(authentication(mockAuth(1L, "john_doe")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when update payload fails validation")
    void shouldReturn400WhenUpdateValidationFails() throws Exception {
        UpdateUserRequest invalidRequest = new UpdateUserRequest("ab", "short");

        mockMvc.perform(put("/api/v1/users/1")
                        .with(authentication(mockAuth(1L, "john_doe")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.username").exists())
                .andExpect(jsonPath("$.invalidParams.password").exists());
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /api/v1/users/{id} when requester is owner")
    void shouldDeleteUserSuccessfullyWhenOwner() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when deleting another user's account")
    void shouldReturn403WhenDeletingOtherUser() throws Exception {
        org.mockito.Mockito.doThrow(new UnauthorizedOperationException("You do not have permission to delete account ID 2"))
                .when(deleteUserUseCase).deleteUser(2L, 1L);

        mockMvc.perform(delete("/api/v1/users/2")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent account")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        org.mockito.Mockito.doThrow(new UserNotFoundException(999L))
                .when(deleteUserUseCase).deleteUser(999L, 1L);

        mockMvc.perform(delete("/api/v1/users/999")
                        .with(authentication(mockAuth(1L, "john_doe"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
