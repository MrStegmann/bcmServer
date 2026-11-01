package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreateTeamCommand;
import com.basketapp.bcmserver.application.port.in.CreateTeamUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteTeamUseCase;
import com.basketapp.bcmserver.application.port.in.GetTeamUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateTeamCommand;
import com.basketapp.bcmserver.application.port.in.UpdateTeamUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Team;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateTeamRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.TeamResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateTeamRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.TeamWebMapper;
import com.basketapp.bcmserver.infrastructure.config.AuthenticatedUser;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateTeamUseCase createTeamUseCase;

    @MockBean
    private GetTeamUseCase getTeamUseCase;

    @MockBean
    private UpdateTeamUseCase updateTeamUseCase;

    @MockBean
    private DeleteTeamUseCase deleteTeamUseCase;

    @MockBean
    private TeamWebMapper teamWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    private UsernamePasswordAuthenticationToken mockAuth() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "coach_john");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should return 201 Created and Location header for POST /api/v1/teams")
    void shouldCreateTeamSuccessfullyOnCanonicalPath() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Cadete Femenino A", "U16 Federado");
        CreateTeamCommand command = new CreateTeamCommand("Cadete Femenino A", "U16 Federado");
        Instant now = Instant.now();
        Team team = Team.reconstitute(1L, "Cadete Femenino A", "U16 Federado", now);
        TeamResponse response = new TeamResponse(1L, "Cadete Femenino A", "U16 Federado", now);

        when(teamWebMapper.toCommand(request)).thenReturn(command);
        when(createTeamUseCase.createTeam(command)).thenReturn(team);
        when(teamWebMapper.toResponse(team)).thenReturn(response);

        mockMvc.perform(post("/api/v1/teams")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/teams/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Cadete Femenino A"))
                .andExpect(jsonPath("$.category").value("U16 Federado"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 201 Created for POST /teams (alias path)")
    void shouldCreateTeamSuccessfullyOnAliasPath() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Senior A", "Senior");
        CreateTeamCommand command = new CreateTeamCommand("Senior A", "Senior");
        Team team = Team.reconstitute(2L, "Senior A", "Senior", Instant.now());
        TeamResponse response = new TeamResponse(2L, "Senior A", "Senior", team.getCreatedAt());

        when(teamWebMapper.toCommand(request)).thenReturn(command);
        when(createTeamUseCase.createTeam(command)).thenReturn(team);
        when(teamWebMapper.toResponse(team)).thenReturn(response);

        mockMvc.perform(post("/teams")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/teams/2"))
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Senior A"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token is missing on POST")
    void shouldReturn401WhenUnauthenticatedOnPost() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Any Team", "Category");

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("   ", "Category");

        mockMvc.perform(post("/api/v1/teams")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.name").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when name exceeds 100 characters")
    void shouldReturn400WhenNameExceeds100Characters() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("A".repeat(101), "Category");

        mockMvc.perform(post("/api/v1/teams")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when category exceeds 50 characters")
    void shouldReturn400WhenCategoryExceeds50Characters() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Valid Name", "B".repeat(51));

        mockMvc.perform(post("/api/v1/teams")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 200 OK and list of teams for GET /api/v1/teams")
    void shouldGetAllTeamsSuccessfully() throws Exception {
        Team team1 = Team.reconstitute(1L, "Cadete A", "Junior", Instant.now());
        Team team2 = Team.reconstitute(2L, "Senior B", "Senior", Instant.now());
        List<Team> teams = List.of(team1, team2);
        List<TeamResponse> responses = List.of(
                new TeamResponse(1L, "Cadete A", "Junior", team1.getCreatedAt()),
                new TeamResponse(2L, "Senior B", "Senior", team2.getCreatedAt())
        );

        when(getTeamUseCase.findAll()).thenReturn(teams);
        when(teamWebMapper.toResponseList(teams)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/teams")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cadete A"))
                .andExpect(jsonPath("$[1].name").value("Senior B"));
    }

    @Test
    @DisplayName("Should return 200 OK and team by ID for GET /api/v1/teams/{id}")
    void shouldGetTeamByIdSuccessfully() throws Exception {
        Team team = Team.reconstitute(1L, "Cadete A", "Junior", Instant.now());
        TeamResponse response = new TeamResponse(1L, "Cadete A", "Junior", team.getCreatedAt());

        when(getTeamUseCase.findById(1L)).thenReturn(team);
        when(teamWebMapper.toResponse(team)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Cadete A"))
                .andExpect(jsonPath("$.category").value("Junior"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when team ID does not exist")
    void shouldReturn404WhenTeamNotFound() throws Exception {
        when(getTeamUseCase.findById(999L)).thenThrow(new TeamNotFoundException(999L));

        mockMvc.perform(get("/api/v1/teams/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Team with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when ID is not a number")
    void shouldReturn400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/teams/invalid-id")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when ID is negative or zero")
    void shouldReturn400WhenIdIsNonPositive() throws Exception {
        mockMvc.perform(get("/api/v1/teams/-1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token is missing on GET")
    void shouldReturn401WhenUnauthenticatedOnGet() throws Exception {
        mockMvc.perform(get("/api/v1/teams/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 200 OK and updated team for PUT /api/v1/teams/{id}")
    void shouldUpdateTeamSuccessfullyOnCanonicalPath() throws Exception {
        UpdateTeamRequest request = new UpdateTeamRequest("Senior Femenino Autonomico", "Senior");
        UpdateTeamCommand command = new UpdateTeamCommand(1L, "Senior Femenino Autonomico", "Senior");
        Instant now = Instant.now();
        Team updated = Team.reconstitute(1L, "Senior Femenino Autonomico", "Senior", now);
        TeamResponse response = new TeamResponse(1L, "Senior Femenino Autonomico", "Senior", now);

        when(teamWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updateTeamUseCase.updateTeam(command)).thenReturn(updated);
        when(teamWebMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(put("/api/v1/teams/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Senior Femenino Autonomico"))
                .andExpect(jsonPath("$.category").value("Senior"));
    }

    @Test
    @DisplayName("Should return 200 OK for PUT /teams/{id} (alias path)")
    void shouldUpdateTeamSuccessfullyOnAliasPath() throws Exception {
        UpdateTeamRequest request = new UpdateTeamRequest("Senior Femenino Autonomico", "Senior");
        UpdateTeamCommand command = new UpdateTeamCommand(1L, "Senior Femenino Autonomico", "Senior");
        Team updated = Team.reconstitute(1L, "Senior Femenino Autonomico", "Senior", Instant.now());
        TeamResponse response = new TeamResponse(1L, "Senior Femenino Autonomico", "Senior", updated.getCreatedAt());

        when(teamWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updateTeamUseCase.updateTeam(command)).thenReturn(updated);
        when(teamWebMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(put("/teams/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating non-existent team")
    void shouldReturn404WhenUpdatingNonExistentTeam() throws Exception {
        UpdateTeamRequest request = new UpdateTeamRequest("Updated Name", "Senior");
        UpdateTeamCommand command = new UpdateTeamCommand(999L, "Updated Name", "Senior");

        when(teamWebMapper.toCommand(999L, request)).thenReturn(command);
        when(updateTeamUseCase.updateTeam(command)).thenThrow(new TeamNotFoundException(999L));

        mockMvc.perform(put("/api/v1/teams/999")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when update payload has blank name")
    void shouldReturn400WhenUpdatePayloadHasBlankName() throws Exception {
        UpdateTeamRequest request = new UpdateTeamRequest("   ", "Senior");

        mockMvc.perform(put("/api/v1/teams/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token missing on PUT")
    void shouldReturn401WhenUnauthenticatedOnPut() throws Exception {
        UpdateTeamRequest request = new UpdateTeamRequest("Updated Name", "Senior");

        mockMvc.perform(put("/api/v1/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /api/v1/teams/{id}")
    void shouldDeleteTeamSuccessfullyOnCanonicalPath() throws Exception {
        doNothing().when(deleteTeamUseCase).deleteTeam(1L);

        mockMvc.perform(delete("/api/v1/teams/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /teams/{id} (alias path)")
    void shouldDeleteTeamSuccessfullyOnAliasPath() throws Exception {
        doNothing().when(deleteTeamUseCase).deleteTeam(2L);

        mockMvc.perform(delete("/teams/2")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent team")
    void shouldReturn404WhenDeletingNonExistentTeam() throws Exception {
        doThrow(new TeamNotFoundException(999L)).when(deleteTeamUseCase).deleteTeam(999L);

        mockMvc.perform(delete("/api/v1/teams/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when DELETE ID is not numeric")
    void shouldReturn400WhenDeleteIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/v1/teams/abc")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when DELETE ID is non-positive")
    void shouldReturn400WhenDeleteIdIsNonPositive() throws Exception {
        mockMvc.perform(delete("/api/v1/teams/-1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token missing on DELETE")
    void shouldReturn401WhenUnauthenticatedOnDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/teams/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
