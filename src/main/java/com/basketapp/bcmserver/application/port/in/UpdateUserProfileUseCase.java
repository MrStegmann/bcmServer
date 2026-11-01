package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.User;

/**
 * Inbound port for updating user profile and credentials with ownership enforcement.
 */
public interface UpdateUserProfileUseCase {

    User updateProfile(UpdateUserCommand command);
}

