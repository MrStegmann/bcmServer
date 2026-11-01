package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.application.port.out.GameRepositoryPort;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.GamePersistenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter bridging GameRepositoryPort to SpringDataGameRepository.
 */
@Component
public class GameRepositoryAdapter implements GameRepositoryPort {

    private final SpringDataGameRepository jpaRepository;
    private final GamePersistenceMapper mapper;

    public GameRepositoryAdapter(SpringDataGameRepository jpaRepository, GamePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Game save(Game game) {
        GameJpaEntity entity = mapper.toEntity(game);
        GameJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findByTeamId(Long teamId) {
        return mapper.toDomainList(jpaRepository.findByTeamId(teamId));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}

