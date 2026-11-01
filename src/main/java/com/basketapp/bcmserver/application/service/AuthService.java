package com.basketapp.bcmserver.application.service;

import com.basketapp.bcmserver.application.port.in.AuthToken;
import com.basketapp.bcmserver.application.port.in.AuthenticationResult;
import com.basketapp.bcmserver.application.port.in.LoginUserCommand;
import com.basketapp.bcmserver.application.port.in.LoginUserUseCase;
import com.basketapp.bcmserver.application.port.out.PasswordEncoderPort;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.application.port.out.UserRepositoryPort;
import com.basketapp.bcmserver.domain.exception.AccountStatusException;
import com.basketapp.bcmserver.domain.exception.InvalidCredentialsException;
import com.basketapp.bcmserver.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service managing authentication and token issuance.
 */
@Service
public class AuthService implements LoginUserUseCase {

    private static final String DUMMY_HASH = "$2a$12$e/sampleHashedPasswordToEqualizeResponseTiming000000000";

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthService(
            UserRepositoryPort userRepository,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult login(LoginUserCommand command) {
        if (command.username() == null || command.username().isBlank() ||
                command.password() == null || command.password().isBlank()) {
            throw new InvalidCredentialsException();
        }

        Optional<User> userOpt = userRepository.findByUsername(command.username());
        if (userOpt.isEmpty()) {
            // Anti-enumeration: compute dummy hash match to equalize timing profile
            passwordEncoder.matches(command.password(), DUMMY_HASH);
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEligibleForLogin()) {
            throw new AccountStatusException("Account is locked or inactive");
        }

        AuthToken authToken = tokenProvider.generateToken(user);
        return new AuthenticationResult(authToken, user);
    }
}
