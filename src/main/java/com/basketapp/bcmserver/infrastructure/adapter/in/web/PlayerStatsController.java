package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsUseCase;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerStatsRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerStatsResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerStatsRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerStatsWebMapper;
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
 * REST controller managing protected player statistics operations.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping({"/api/player-stats", "/player-stats", "/api/v1/player-stats"})
@Tag(name = "Player Stats", description = "Protected player statistics management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PlayerStatsController {

    private final CreatePlayerStatsUseCase createPlayerStatsUseCase;
    private final GetPlayerStatsUseCase getPlayerStatsUseCase;
    private final UpdatePlayerStatsUseCase updatePlayerStatsUseCase;
    private final DeletePlayerStatsUseCase deletePlayerStatsUseCase;
    private final PlayerStatsWebMapper playerStatsWebMapper;

    public PlayerStatsController(
            CreatePlayerStatsUseCase createPlayerStatsUseCase,
            GetPlayerStatsUseCase getPlayerStatsUseCase,
            UpdatePlayerStatsUseCase updatePlayerStatsUseCase,
            DeletePlayerStatsUseCase deletePlayerStatsUseCase,
            PlayerStatsWebMapper playerStatsWebMapper
    ) {
        this.createPlayerStatsUseCase = createPlayerStatsUseCase;
        this.getPlayerStatsUseCase = getPlayerStatsUseCase;
        this.updatePlayerStatsUseCase = updatePlayerStatsUseCase;
        this.deletePlayerStatsUseCase = deletePlayerStatsUseCase;
        this.playerStatsWebMapper = playerStatsWebMapper;
    }

    @PostMapping
    @Operation(summary = "Create player statistics record", description = "Creates a new statistical record for a player in a specific basketball game. Requires valid JWT authentication. Request payload is validated prior to database access.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Player statistics created successfully",
                    headers = @Header(name = "Location", description = "URI of the newly created player stats resource", schema = @Schema(type = "string", example = "/api/player-stats/1")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or parameter validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired JWT bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Referenced game or player does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerStatsResponse> createPlayerStats(@Valid @RequestBody CreatePlayerStatsRequest request) {
        CreatePlayerStatsCommand command = playerStatsWebMapper.toCommand(request);
        PlayerStats created = createPlayerStatsUseCase.createPlayerStats(command);
        URI location = URI.create("/api/player-stats/" + created.getId());
        return ResponseEntity.created(location).body(playerStatsWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List player statistics", description = "Retrieves player statistics with optional filtering by game or by player. Requires valid JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection of player statistics records",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PlayerStatsResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter format",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired JWT bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<PlayerStatsResponse>> getPlayerStats(
            @Parameter(description = "Filter statistics by game identifier", example = "10")
            @RequestParam(required = false) @Positive(message = "Game ID must be a positive number") Long gameId,
            @Parameter(description = "Filter statistics by player identifier", example = "4")
            @RequestParam(required = false) @Positive(message = "Player ID must be a positive number") Long playerId
    ) {
        List<PlayerStats> stats = (gameId != null || playerId != null)
                ? getPlayerStatsUseCase.getPlayerStatsByFilters(gameId, playerId)
                : getPlayerStatsUseCase.getAllPlayerStats();
        return ResponseEntity.ok(playerStatsWebMapper.toResponseList(stats));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player statistics by ID", description = "Retrieves detailed statistics for a single player stats record. Requires valid JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player statistics record found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID format (must be a positive integer)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired JWT bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Player statistics record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerStatsResponse> getPlayerStatsById(
            @Parameter(description = "Unique identifier of the player statistics record", example = "1")
            @PathVariable @Positive(message = "Player stats ID must be a positive number") Long id
    ) {
        PlayerStats stats = getPlayerStatsUseCase.getPlayerStatsById(id);
        return ResponseEntity.ok(playerStatsWebMapper.toResponse(stats));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update player statistics", description = "Updates an existing player statistics record. Requires valid JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player statistics record updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlayerStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or parameter validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired JWT bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Player statistics record, referenced game, or player not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlayerStatsResponse> updatePlayerStats(
            @Parameter(description = "Unique identifier of the player statistics record to update", example = "1")
            @PathVariable @Positive(message = "Player stats ID must be a positive number") Long id,
            @Valid @RequestBody UpdatePlayerStatsRequest request
    ) {
        UpdatePlayerStatsCommand command = playerStatsWebMapper.toCommand(id, request);
        PlayerStats updated = updatePlayerStatsUseCase.updatePlayerStats(command);
        return ResponseEntity.ok(playerStatsWebMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete player statistics", description = "Permanently deletes a player statistics record. Requires valid JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Player statistics record deleted successfully (No Content)"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format (must be a positive integer)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired JWT bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Player statistics record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deletePlayerStats(
            @Parameter(description = "Unique identifier of the player statistics record to delete", example = "1")
            @PathVariable @Positive(message = "Player stats ID must be a positive number") Long id
    ) {
        deletePlayerStatsUseCase.deletePlayerStats(id);
        return ResponseEntity.noContent().build();
    }
}
