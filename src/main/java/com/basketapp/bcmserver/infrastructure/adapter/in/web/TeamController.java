package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreateTeamCommand;
import com.basketapp.bcmserver.application.port.in.CreateTeamUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteTeamUseCase;
import com.basketapp.bcmserver.application.port.in.GetTeamUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateTeamCommand;
import com.basketapp.bcmserver.application.port.in.UpdateTeamUseCase;
import com.basketapp.bcmserver.domain.model.Team;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateTeamRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.TeamResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateTeamRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.TeamWebMapper;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing protected team operations.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping({"/api/v1/teams", "/teams"})
@Tag(name = "Teams", description = "Protected team management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class TeamController {

    private final CreateTeamUseCase createTeamUseCase;
    private final GetTeamUseCase getTeamUseCase;
    private final UpdateTeamUseCase updateTeamUseCase;
    private final DeleteTeamUseCase deleteTeamUseCase;
    private final TeamWebMapper teamWebMapper;

    public TeamController(
            CreateTeamUseCase createTeamUseCase,
            GetTeamUseCase getTeamUseCase,
            UpdateTeamUseCase updateTeamUseCase,
            DeleteTeamUseCase deleteTeamUseCase,
            TeamWebMapper teamWebMapper
    ) {
        this.createTeamUseCase = createTeamUseCase;
        this.getTeamUseCase = getTeamUseCase;
        this.updateTeamUseCase = updateTeamUseCase;
        this.deleteTeamUseCase = deleteTeamUseCase;
        this.teamWebMapper = teamWebMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new team", description = "Creates a new basketball team. Validates JWT before parsing/validating request payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team successfully created",
                    headers = @Header(name = "Location", description = "URI of the newly created team", schema = @Schema(type = "string", example = "/api/v1/teams/1")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        CreateTeamCommand command = teamWebMapper.toCommand(request);
        Team created = createTeamUseCase.createTeam(command);
        URI location = URI.create("/api/v1/teams/" + created.getId());
        return ResponseEntity.created(location).body(teamWebMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List all teams", description = "Retrieves a list of all active basketball teams. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of teams",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = TeamResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        List<Team> teams = getTeamUseCase.findAll();
        return ResponseEntity.ok(teamWebMapper.toResponseList(teams));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID", description = "Retrieves a single team profile by ID. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed team ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TeamResponse> getTeamById(
            @Parameter(description = "Unique numeric identifier of the team", example = "1")
            @PathVariable @Positive(message = "Team ID must be a positive number") Long id
    ) {
        Team team = getTeamUseCase.findById(id);
        return ResponseEntity.ok(teamWebMapper.toResponse(team));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update team by ID", description = "Updates the name and/or category of an existing team. Validates JWT before payload inspection.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TeamResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request payload failed validation or missing mandatory fields",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TeamResponse> updateTeam(
            @Parameter(description = "Unique numeric identifier of the team", example = "1")
            @PathVariable @Positive(message = "Team ID must be a positive number") Long id,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        UpdateTeamCommand command = teamWebMapper.toCommand(id, request);
        Team updated = updateTeamUseCase.updateTeam(command);
        return ResponseEntity.ok(teamWebMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete team by ID", description = "Deletes a team and cascades deletion to associated players and games. Requires valid JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Team deleted successfully (no content returned)"),
            @ApiResponse(responseCode = "400", description = "Malformed team ID parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "Unique numeric identifier of the team", example = "1")
            @PathVariable @Positive(message = "Team ID must be a positive number") Long id
    ) {
        deleteTeamUseCase.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}
