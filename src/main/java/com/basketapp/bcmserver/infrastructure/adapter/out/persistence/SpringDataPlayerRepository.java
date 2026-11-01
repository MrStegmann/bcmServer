package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPlayerRepository extends JpaRepository<PlayerJpaEntity, Long> {

    List<PlayerJpaEntity> findByTeamId(Long teamId);
}

