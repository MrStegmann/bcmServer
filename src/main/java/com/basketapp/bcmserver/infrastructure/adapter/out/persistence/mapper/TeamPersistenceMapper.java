package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper;

import com.basketapp.bcmserver.domain.model.Team;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.TeamJpaEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between Team domain entity and TeamJpaEntity persistence model.
 */
@Mapper(componentModel = "spring")
public interface TeamPersistenceMapper {

    TeamJpaEntity toEntity(Team domain);

    default Team toDomain(TeamJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Team.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getCreatedAt()
        );
    }
}

