package com.basketapp.bcmserver.infrastructure.adapter.out.persistence.mapper;

import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.domain.model.UserStatus;
import com.basketapp.bcmserver.infrastructure.adapter.out.persistence.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between User domain entity and UserJpaEntity persistence model.
 */
@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    @Mapping(target = "password", source = "passwordHash")
    @Mapping(target = "status", expression = "java(domain.getStatus() != null ? domain.getStatus().name() : \"ACTIVE\")")
    UserJpaEntity toEntity(User domain);

    default User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        UserStatus status = UserStatus.ACTIVE;
        if (entity.getStatus() != null) {
            try {
                status = UserStatus.valueOf(entity.getStatus());
            } catch (IllegalArgumentException e) {
                status = UserStatus.ACTIVE;
            }
        }
        return User.reconstitute(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                status,
                entity.getCreatedAt()
        );
    }
}
