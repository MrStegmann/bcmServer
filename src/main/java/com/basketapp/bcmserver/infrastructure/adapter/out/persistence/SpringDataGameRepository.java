package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataGameRepository extends JpaRepository<GameJpaEntity, Long> {

    List<GameJpaEntity> findByTeamId(Long teamId);
}

