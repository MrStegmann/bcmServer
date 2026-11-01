package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper;

import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.GameJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper between Game domain entity and GameJpaEntity persistence model.
 */
@Mapper(componentModel = "spring")
public interface GamePersistenceMapper {

    @Mapping(target = "status", expression = "java(domain.getStatus() != null ? domain.getStatus().getValue() : null)")
    GameJpaEntity toEntity(Game domain);

    default Game toDomain(GameJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        GameStatus status = entity.getStatus() != null ? GameStatus.fromValue(entity.getStatus()) : GameStatus.PENDING;
        return Game.reconstitute(
                entity.getId(),
                entity.getTeamId(),
                entity.getOpponent(),
                entity.getRound(),
                entity.getDate(),
                status,
                entity.getCreatedAt(),
                entity.getResultC1(),
                entity.getResultC2(),
                entity.getResultC3(),
                entity.getResultC4(),
                entity.getResultExtra(),
                entity.getResultC1Opponent(),
                entity.getResultC2Opponent(),
                entity.getResultC3Opponent(),
                entity.getResultC4Opponent(),
                entity.getResultExtraOpponent(),
                entity.getFoulsC1(),
                entity.getFoulsC2(),
                entity.getFoulsC3(),
                entity.getFoulsC4(),
                entity.getFoulsExtra(),
                entity.getFoulsC1Opponent(),
                entity.getFoulsC2Opponent(),
                entity.getFoulsC3Opponent(),
                entity.getFoulsC4Opponent(),
                entity.getFoulsExtraOpponent()
        );
    }

    default List<Game> toDomainList(List<GameJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}

