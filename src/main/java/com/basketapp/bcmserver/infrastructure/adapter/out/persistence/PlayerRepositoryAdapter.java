package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.application.port.out.PlayerRepositoryPort;
import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerPersistenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter bridging PlayerRepositoryPort to SpringDataPlayerRepository.
 */
@Component
public class PlayerRepositoryAdapter implements PlayerRepositoryPort {

    private final SpringDataPlayerRepository jpaRepository;
    private final PlayerPersistenceMapper mapper;

    public PlayerRepositoryAdapter(SpringDataPlayerRepository jpaRepository, PlayerPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Player save(Player player) {
        PlayerJpaEntity entity = mapper.toEntity(player);
        PlayerJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findByTeamId(Long teamId) {
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

