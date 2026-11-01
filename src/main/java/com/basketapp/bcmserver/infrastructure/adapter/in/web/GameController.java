package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreateGameCommand;
import com.basketapp.bcmserver.application.port.in.CreateGameUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteGameUseCase;
import com.basketapp.bcmserver.application.port.in.GetGameUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateGameCommand;
import com.basketapp.bcmserver.application.port.in.UpdateGameUseCase;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateGameRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.GameResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateGameRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.GameWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing protected basketball game operations.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping({"/api/games", "/games", "/api/v1/games"})
@Tag(name = "Games", description = "Protected game management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class GameController {

    private final CreateGameUseCase createGameUseCase;
    private final GetGameUseCase getGameUseCase;
    private final UpdateGameUseCase updateGameUseCase;
    private final DeleteGameUseCase deleteGameUseCase;
    private final GameWebMapper gameWebMapper;

    public GameController(
            CreateGameUseCase createGameUseCase,
            GetGameUseCase getGameUseCase,
            UpdateGameUseCase updateGameUseCase,
            DeleteGameUseCase deleteGameUseCase,
            GameWebMapper gameWebMapper
    ) {
        this.createGameUseCase = createGameUseCase;
        this.getGameUseCase = getGameUseCase;
        this.updateGameUseCase = updateGameUseCase;
        this.deleteGameUseCase = deleteGameUseCase;
        this.gameWebMapper = gameWebMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new game", description = "Creates a new basketball game for an existing team. Enforces JWT authentication precedence before request payload validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Game successfully created",
                    headers = @Header(name = "Location", description = "URI of the newly created game resource", schema = @Schema(type = "string", example = "/api/games/1")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Associated team specified in team_id not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        CreateGameCommand command = gameWebMapper.toCommand(request);
        Game created = createGameUseCase.createGame(command);
        URI location = URI.create("/api/games/" + created.getId());
        return ResponseEntity.created(location).body(gameWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List all games or filter by team", description = "Retrieves a list of games. If teamId query parameter is provided, filters games belonging to that specific team. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of games",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = GameResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Malformed query parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<GameResponse>> getGames(
            @Parameter(description = "Optional filter to retrieve only games belonging to the specified team", example = "1")
            @RequestParam(required = false) @Positive(message = "Team ID must be a positive number") Long teamId
    ) {
        List<Game> games = (teamId != null)
                ? getGameUseCase.findByTeamId(teamId)
                : getGameUseCase.findAll();
        return ResponseEntity.ok(gameWebMapper.toResponseList(games));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get game by ID", description = "Retrieves a single game breakdown by identifier. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed game ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<GameResponse> getGameById(
            @Parameter(description = "Numeric ID of the game to retrieve", example = "1")
            @PathVariable @Positive(message = "Game ID must be a positive number") Long id
    ) {
        Game game = getGameUseCase.findById(id);
        return ResponseEntity.ok(gameWebMapper.toResponse(game));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update match score, status, and attributes", description = "Fully updates game attributes, scores, fouls, and status. Enforces JWT authentication precedence before request payload validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game successfully updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Game or referenced team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<GameResponse> updateGame(
            @Parameter(description = "Numeric ID of the game to update", example = "1")
            @PathVariable @Positive(message = "Game ID must be a positive number") Long id,
            @Valid @RequestBody UpdateGameRequest request
    ) {
        UpdateGameCommand command = gameWebMapper.toCommand(id, request);
        Game updated = updateGameUseCase.updateGame(command);
        return ResponseEntity.ok(gameWebMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a game", description = "Permanently removes a game and cascades deletion to dependent player statistics and cached records. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Game successfully deleted, no content returned"),
            @ApiResponse(responseCode = "400", description = "Malformed game ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteGame(
            @Parameter(description = "Numeric ID of the game to delete", example = "1")
            @PathVariable @Positive(message = "Game ID must be a positive number") Long id
    ) {
        deleteGameUseCase.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}
