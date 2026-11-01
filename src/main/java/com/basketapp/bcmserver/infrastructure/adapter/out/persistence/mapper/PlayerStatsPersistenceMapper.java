package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper;

import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.PlayerStatsJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper between PlayerStats domain entity and PlayerStatsJpaEntity persistence model.
 */
@Mapper(componentModel = "spring")
public interface PlayerStatsPersistenceMapper {

    PlayerStatsJpaEntity toEntity(PlayerStats domain);

    default PlayerStats toDomain(PlayerStatsJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PlayerStats.reconstitute(
                entity.getId(),
                entity.getGameId(),
                entity.getPlayerId(),
                entity.getMinutes(),
                entity.getSeconds(),
                entity.getT1a(),
                entity.getT1i(),
                entity.getT2a(),
                entity.getT2i(),
                entity.getT3a(),
                entity.getT3i(),
                entity.getOreb(),
                entity.getDreb(),
                entity.getAsis(),
                entity.getRec(),
                entity.getPer(),
                entity.getFalt(),
                entity.getValuation()
        );
    }

    default List<PlayerStats> toDomainList(List<PlayerStatsJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}

