package com.basketapp.bcmserver.application.port.out;

import com.basketapp.bcmserver.domain.model.Team;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Team persistence operations.
 */
public interface TeamRepositoryPort {

    Team save(Team team);

    Optional<Team> findById(Long id);

    List<Team> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);
}

