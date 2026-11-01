package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import com.basketapp.bcmserver.application.port.out.PlayerStatsRepositoryPort;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper.PlayerStatsPersistenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter bridging PlayerStatsRepositoryPort to SpringDataPlayerStatsRepository.
 */
@Component
public class PlayerStatsRepositoryAdapter implements PlayerStatsRepositoryPort {

    private final SpringDataPlayerStatsRepository jpaRepository;
    private final PlayerStatsPersistenceMapper mapper;

    public PlayerStatsRepositoryAdapter(SpringDataPlayerStatsRepository jpaRepository, PlayerStatsPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PlayerStats save(PlayerStats playerStats) {
        PlayerStatsJpaEntity entity = mapper.toEntity(playerStats);
        PlayerStatsJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerStats> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> findByGameId(Long gameId) {
        return mapper.toDomainList(jpaRepository.findByGameId(gameId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> findByPlayerId(Long playerId) {
        return mapper.toDomainList(jpaRepository.findByPlayerId(playerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStats> findByGameIdAndPlayerId(Long gameId, Long playerId) {
        return mapper.toDomainList(jpaRepository.findByGameIdAndPlayerId(gameId, playerId));
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

