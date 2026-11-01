package com.basketapp.bcmserver.application.port.in;

/**
 * Inbound port for user login and credential verification use case.
 */
public interface LoginUserUseCase {

    AuthenticationResult login(LoginUserCommand command);
}

