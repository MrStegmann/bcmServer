package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.User;

/**
 * Inbound port for user registration use case.
 */
public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}

