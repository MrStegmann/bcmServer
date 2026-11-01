package com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper;

import com.basketapp.bcmserver.application.port.in.CreateTeamCommand;
import com.basketapp.bcmserver.application.port.in.UpdateTeamCommand;
import com.basketapp.bcmserver.domain.model.Team;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateTeamRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.TeamResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateTeamRequest;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper converting between web DTOs, application commands, and domain models.
 */
@Mapper(componentModel = "spring")
public interface TeamWebMapper {

    CreateTeamCommand toCommand(CreateTeamRequest request);

    default UpdateTeamCommand toCommand(Long id, UpdateTeamRequest request) {
        if (request == null) {
            return new UpdateTeamCommand(id, null, null);
        }
        return new UpdateTeamCommand(id, request.name(), request.category());
    }

    TeamResponse toResponse(Team team);

    List<TeamResponse> toResponseList(List<Team> teams);
}
