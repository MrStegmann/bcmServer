package com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper;

import com.basketapp.bcmserver.application.port.in.CreateGameCommand;
import com.basketapp.bcmserver.application.port.in.UpdateGameCommand;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateGameRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.GameResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateGameRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper converting between web DTOs, application commands, and domain models.
 */
@Mapper(componentModel = "spring")
public interface GameWebMapper {

    CreateGameCommand toCommand(CreateGameRequest request);

    default UpdateGameCommand toCommand(Long id, UpdateGameRequest request) {
        if (request == null) {
            return new UpdateGameCommand(id, null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null);
        }
        return new UpdateGameCommand(
                id,
                request.teamId(),
                request.opponent(),
                request.round(),
                request.date(),
                request.status(),
                request.resultC1(),
                request.resultC2(),
                request.resultC3(),
                request.resultC4(),
                request.resultExtra(),
                request.resultC1Opponent(),
                request.resultC2Opponent(),
                request.resultC3Opponent(),
                request.resultC4Opponent(),
                request.resultExtraOpponent(),
                request.foulsC1(),
                request.foulsC2(),
                request.foulsC3(),
                request.foulsC4(),
                request.foulsExtra(),
                request.foulsC1Opponent(),
                request.foulsC2Opponent(),
                request.foulsC3Opponent(),
                request.foulsC4Opponent(),
                request.foulsExtraOpponent()
        );
    }

    @Mapping(target = "status", expression = "java(game.getStatus() != null ? game.getStatus().getValue() : null)")
    GameResponse toResponse(Game game);

    List<GameResponse> toResponseList(List<Game> games);
}

