package com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper;

import com.basketapp.bcmserver.application.port.in.AuthenticationResult;
import com.basketapp.bcmserver.application.port.in.LoginUserCommand;
import com.basketapp.bcmserver.application.port.in.RegisterUserCommand;
import com.basketapp.bcmserver.application.port.in.UpdateUserCommand;
import com.basketapp.bcmserver.domain.model.User;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.AuthResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.LoginRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between web DTOs, application commands, and domain models.
 * Strictly guarantees that password hashes are not mapped into public responses.
 */
@Mapper(componentModel = "spring")
public interface UserWebMapper {

    RegisterUserCommand toCommand(RegisterRequest request);

    LoginUserCommand toCommand(LoginRequest request);

    default UpdateUserCommand toCommand(Long targetUserId, Long requesterUserId, UpdateUserRequest request) {
        if (request == null) {
            return new UpdateUserCommand(targetUserId, requesterUserId, null, null);
        }
        return new UpdateUserCommand(targetUserId, requesterUserId, request.username(), request.password());
    }

    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : \"ACTIVE\")")
    UserProfileResponse toProfileResponse(User user);

    default AuthResponse toAuthResponse(AuthenticationResult result) {
        if (result == null) {
            return null;
        }
        return new AuthResponse(
                result.token().token(),
                result.token().tokenType(),
                result.token().expiresIn(),
                toProfileResponse(result.user())
        );
    }
}
