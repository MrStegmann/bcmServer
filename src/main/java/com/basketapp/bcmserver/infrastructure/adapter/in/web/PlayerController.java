package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerUseCase;
import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerWebMapper;
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
 * REST controller managing protected player operations.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping({"/api/v1/players", "/players"})
@Tag(name = "Players", description = "Protected player management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PlayerController {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final GetPlayerUseCase getPlayerUseCase;
    private final UpdatePlayerUseCase updatePlayerUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;
    private final PlayerWebMapper playerWebMapper;

    public PlayerController(
            CreatePlayerUseCase createPlayerUseCase,
            GetPlayerUseCase getPlayerUseCase,
            UpdatePlayerUseCase updatePlayerUseCase,
            DeletePlayerUseCase deletePlayerUseCase,
            PlayerWebMapper playerWebMapper
    ) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.getPlayerUseCase = getPlayerUseCase;
        this.updatePlayerUseCase = updatePlayerUseCase;
        this.deletePlayerUseCase = deletePlayerUseCase;
        this.playerWebMapper = playerWebMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new player", description = "Registers a new basketball player on an existing team. Validates JWT before evaluating request payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Player successfully created",
                    headers = @Header(name = "Location", description = "URI of the newly created player resource", schema = @Schema(type = "string", example = "/api/v1/players/1")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Associated team specified in teamId not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        CreatePlayerCommand command = playerWebMapper.toCommand(request);
        Player created = createPlayerUseCase.createPlayer(command);
        URI location = URI.create("/api/v1/players/" + created.getId());
        return ResponseEntity.created(location).body(playerWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List all players or filter by team", description = "Retrieves a list of players. If teamId query parameter is provided, filters players belonging to that specific team. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of players",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PlayerResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Malformed query parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<PlayerResponse>> getPlayers(
            @Parameter(description = "Optional filter to retrieve only players belonging to the specified team", example = "1")
            @RequestParam(required = false) @Positive(message = "Team ID must be a positive number") Long teamId
    ) {
        List<Player> players = (teamId != null)
                ? getPlayerUseCase.findByTeamId(teamId)
                : getPlayerUseCase.findAll();
        return ResponseEntity.ok(playerWebMapper.toResponseList(players));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player by ID", description = "Retrieves a single player profile by identifier. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed player ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Player not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerResponse> getPlayerById(
            @Parameter(description = "Unique numeric identifier of the player", example = "1")
            @PathVariable @Positive(message = "Player ID must be a positive number") Long id
    ) {
        Player player = getPlayerUseCase.findById(id);
        return ResponseEntity.ok(playerWebMapper.toResponse(player));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update player by ID", description = "Modifies details of an existing player (team assignment, name, jersey number, jersey name). Validates JWT before payload evaluation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Targeted resource (player or referenced team) was not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerResponse> updatePlayer(
            @Parameter(description = "Unique numeric identifier of the player", example = "1")
            @PathVariable @Positive(message = "Player ID must be a positive number") Long id,
            @Valid @RequestBody UpdatePlayerRequest request
    ) {
        UpdatePlayerCommand command = playerWebMapper.toCommand(id, request);
        Player updated = updatePlayerUseCase.updatePlayer(command);
        return ResponseEntity.ok(playerWebMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete player by ID", description = "Deletes an existing player record and cascades deletion to associated player stats. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Player deleted successfully (no content returned)"),
            @ApiResponse(responseCode = "400", description = "Malformed player ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Player not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deletePlayer(
            @Parameter(description = "Unique numeric identifier of the player", example = "1")
            @PathVariable @Positive(message = "Player ID must be a positive number") Long id
    ) {
        deletePlayerUseCase.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}

