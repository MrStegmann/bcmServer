package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper;

import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.PlayerJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper between Player domain entity and PlayerJpaEntity persistence model.
 */
@Mapper(componentModel = "spring")
public interface PlayerPersistenceMapper {

    PlayerJpaEntity toEntity(Player domain);

    default Player toDomain(PlayerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Player.reconstitute(
                entity.getId(),
                entity.getTeamId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getNumber(),
                entity.getJerseyName(),
                entity.getCreatedAt()
        );
    }

    default List<Player> toDomainList(List<PlayerJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}

