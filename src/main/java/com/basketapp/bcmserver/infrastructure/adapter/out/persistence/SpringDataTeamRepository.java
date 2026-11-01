package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for TeamJpaEntity.
 */
@Repository
public interface SpringDataTeamRepository extends JpaRepository<TeamJpaEntity, Long> {
}

