package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.application.port.in.AuthToken;
import com.basketapp.bcmserver.domain.model.User;

/**
 * Outbound port for token generation and verification.
 */
public interface TokenProviderPort {

    AuthToken generateToken(User user);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    Long getUserIdFromToken(String token);
}

