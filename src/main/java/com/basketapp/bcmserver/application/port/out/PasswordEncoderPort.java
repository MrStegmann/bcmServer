package com.basketapp.bcmserver.application.port.out;

/**
 * Outbound port for cryptographic password hashing and verification.
 */
public interface PasswordEncoderPort {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}

