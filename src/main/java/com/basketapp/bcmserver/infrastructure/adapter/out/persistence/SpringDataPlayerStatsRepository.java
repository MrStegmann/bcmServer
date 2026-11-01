package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPlayerStatsRepository extends JpaRepository<PlayerStatsJpaEntity, Long> {

    List<PlayerStatsJpaEntity> findByGameId(Long gameId);

    List<PlayerStatsJpaEntity> findByPlayerId(Long playerId);

    List<PlayerStatsJpaEntity> findByGameIdAndPlayerId(Long gameId, Long playerId);
}

