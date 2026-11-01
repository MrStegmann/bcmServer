package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.DeleteUserUseCase;
import com.basketapp.bcmserver.application.port.in.GetUserProfileUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateUserCommand;
import com.basketapp.bcmserver.application.port.in.UpdateUserProfileUseCase;
import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UserProfileResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.UserWebMapper;
import com.basketapp.bcmserver.infrastructure.config.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing protected user account operations.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Protected user profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserWebMapper userWebMapper;

    public UserController(
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            DeleteUserUseCase deleteUserUseCase,
            UserWebMapper userWebMapper
    ) {
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.userWebMapper = userWebMapper;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the profile of the currently authenticated user based on JWT bearer claims")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        User user = getUserProfileUseCase.getProfileById(currentUser.id());
        return ResponseEntity.ok(userWebMapper.toProfileResponse(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID", description = "Retrieves public user profile details by numeric user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserProfileResponse> getUserById(
            @Parameter(description = "Numeric ID of the target user", example = "1")
            @PathVariable Long id
    ) {
        User user = getUserProfileUseCase.getProfileById(id);
        return ResponseEntity.ok(userWebMapper.toProfileResponse(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile", description = "Updates username and/or password with self-service ownership enforcement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Attempted modification of another user's account",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "New username already taken",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserProfileResponse> updateUser(
            @Parameter(description = "Numeric ID of the user to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        UpdateUserCommand command = userWebMapper.toCommand(id, currentUser.id(), request);
        User updatedUser = updateUserProfileUseCase.updateProfile(command);
        return ResponseEntity.ok(userWebMapper.toProfileResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account", description = "Permanently deletes user account with self-service ownership enforcement")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account permanently deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Attempted deletion of another user's account",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Numeric ID of the user to delete", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        deleteUserUseCase.deleteUser(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}

