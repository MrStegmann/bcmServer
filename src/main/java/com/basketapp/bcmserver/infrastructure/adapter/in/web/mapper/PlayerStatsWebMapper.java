package com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsCommand;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerStatsRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerStatsResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerStatsRequest;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper converting between web DTOs, application commands, and domain models.
 */
@Mapper(componentModel = "spring")
public interface PlayerStatsWebMapper {

    CreatePlayerStatsCommand toCommand(CreatePlayerStatsRequest request);

    default UpdatePlayerStatsCommand toCommand(Long id, UpdatePlayerStatsRequest request) {
        if (request == null) {
            return new UpdatePlayerStatsCommand(id, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new UpdatePlayerStatsCommand(
                id,
                request.gameId(),
                request.playerId(),
                request.minutes(),
                request.seconds(),
                request.t1a(),
                request.t1i(),
                request.t2a(),
                request.t2i(),
                request.t3a(),
                request.t3i(),
                request.oreb(),
                request.dreb(),
                request.asis(),
                request.rec(),
                request.per(),
                request.falt(),
                request.valuation()
        );
    }

    PlayerStatsResponse toResponse(PlayerStats stats);

    List<PlayerStatsResponse> toResponseList(List<PlayerStats> statsList);
}
