package com.basketapp.bcmserver.application.port.in;

import com.basketapp.bcmserver.domain.model.User;

/**
 * Result of user authentication containing generated token and domain user.
 */
public record AuthenticationResult(
        AuthToken token,
        User user
) {}

