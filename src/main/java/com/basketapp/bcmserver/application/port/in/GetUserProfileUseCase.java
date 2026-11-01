package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.User;

/**
 * Inbound port for retrieving user profile details.
 */
public interface GetUserProfileUseCase {

    User getProfileById(Long id);

    User getProfileByUsername(String username);
}

