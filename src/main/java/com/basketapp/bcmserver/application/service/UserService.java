package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.DeleteUserUseCase;
import com.basketapp.bcmserver.application.port.in.GetUserProfileUseCase;
import com.basketapp.bcmserver.application.port.in.RegisterUserCommand;
import com.basketapp.bcmserver.application.port.in.RegisterUserUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateUserCommand;
import com.basketapp.bcmserver.application.port.in.UpdateUserProfileUseCase;
import com.basketapp.bcmserver.application.port.out.PasswordEncoderPort;
import com.basketapp.bcmserver.application.port.out.UserRepositoryPort;
import com.basketapp.bcmserver.domain.exception.SemanticValidationException;
import com.basketapp.bcmserver.domain.exception.UnauthorizedOperationException;
import com.basketapp.bcmserver.domain.exception.UserAlreadyExistsException;
import com.basketapp.bcmserver.domain.exception.UserNotFoundException;
import com.basketapp.bcmserver.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Application service implementing user management use cases.
 */
@Service
public class UserService implements RegisterUserUseCase, GetUserProfileUseCase, UpdateUserProfileUseCase, DeleteUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public UserService(UserRepositoryPort userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        if (command.password() == null || command.password().isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (command.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        validatePasswordComplexity(command.password());

        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException(command.username());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        User user = User.create(command.username(), passwordHash);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfileById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfileByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    @Transactional
    public User updateProfile(UpdateUserCommand command) {
        if (command.requesterUserId() == null || !command.requesterUserId().equals(command.targetUserId())) {
            throw new UnauthorizedOperationException("You do not have permission to update account ID " + command.targetUserId());
        }

        User user = userRepository.findById(command.targetUserId())
                .orElseThrow(() -> new UserNotFoundException(command.targetUserId()));

        if (command.username() != null && !command.username().isBlank()) {
            String newUsername = command.username().trim();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new UserAlreadyExistsException(newUsername);
                }
                user.updateUsername(newUsername);
            }
        }

        if (command.password() != null && !command.password().isBlank()) {
            if (command.password().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            validatePasswordComplexity(command.password());
            String newPasswordHash = passwordEncoder.encode(command.password());
            user.updatePasswordHash(newPasswordHash);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId, Long requesterUserId) {
        if (requesterUserId == null || !requesterUserId.equals(targetUserId)) {
            throw new UnauthorizedOperationException("You do not have permission to delete account ID " + targetUserId);
        }

        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new UserNotFoundException(targetUserId);
        }

        userRepository.deleteById(targetUserId);
    }

    private void validatePasswordComplexity(String password) {
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new SemanticValidationException(
                    "Password does not meet minimum security requirements",
                    Map.of("password", "Password is too weak. Must contain uppercase, lowercase, numbers, and special symbols")
            );
        }
    }
}
