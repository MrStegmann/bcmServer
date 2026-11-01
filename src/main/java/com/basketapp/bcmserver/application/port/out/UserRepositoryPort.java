package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.domain.model.User;

import java.util.Optional;

/**
 * Outbound port for User persistence operations.
 */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteById(Long id);
}

