package com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper;

import com.basketapp.bcmserver.application.port.in.CreatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerCommand;
import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerRequest;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper converting between web DTOs, application commands, and domain models.
 */
@Mapper(componentModel = "spring")
public interface PlayerWebMapper {

    CreatePlayerCommand toCommand(CreatePlayerRequest request);

    default UpdatePlayerCommand toCommand(Long id, UpdatePlayerRequest request) {
        if (request == null) {
            return new UpdatePlayerCommand(id, null, null, null, null, null);
        }
        return new UpdatePlayerCommand(
                id,
                request.teamId(),
                request.firstName(),
                request.lastName(),
                request.number(),
                request.jerseyName()
        );
    }

    PlayerResponse toResponse(Player player);

    List<PlayerResponse> toResponseList(List<Player> players);
}
