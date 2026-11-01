package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Player;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerWebMapper;
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

@WebMvcTest(PlayerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePlayerUseCase createPlayerUseCase;

    @MockBean
    private GetPlayerUseCase getPlayerUseCase;

    @MockBean
    private UpdatePlayerUseCase updatePlayerUseCase;

    @MockBean
    private DeletePlayerUseCase deletePlayerUseCase;

    @MockBean
    private PlayerWebMapper playerWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    private UsernamePasswordAuthenticationToken mockAuth() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "coach_john");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should return 201 Created and Location header for POST /api/v1/players")
    void shouldCreatePlayerSuccessfullyOnCanonicalPath() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "Michael", "Jordan", "23", "JORDAN");
        CreatePlayerCommand command = new CreatePlayerCommand(1L, "Michael", "Jordan", "23", "JORDAN");
        Instant now = Instant.now();
        Player player = Player.reconstitute(10L, 1L, "Michael", "Jordan", "23", "JORDAN", now);
        PlayerResponse response = new PlayerResponse(10L, 1L, "Michael", "Jordan", "23", "JORDAN", now);

        when(playerWebMapper.toCommand(request)).thenReturn(command);
        when(createPlayerUseCase.createPlayer(command)).thenReturn(player);
        when(playerWebMapper.toResponse(player)).thenReturn(response);

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/players/10"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.teamId").value(1L))
                .andExpect(jsonPath("$.firstName").value("Michael"))
                .andExpect(jsonPath("$.lastName").value("Jordan"))
                .andExpect(jsonPath("$.number").value("23"))
                .andExpect(jsonPath("$.jerseyName").value("JORDAN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 201 Created for POST /players (alias path)")
    void shouldCreatePlayerSuccessfullyOnAliasPath() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "Scottie", "Pippen", "33", "PIPPEN");
        CreatePlayerCommand command = new CreatePlayerCommand(1L, "Scottie", "Pippen", "33", "PIPPEN");
        Instant now = Instant.now();
        Player player = Player.reconstitute(11L, 1L, "Scottie", "Pippen", "33", "PIPPEN", now);
        PlayerResponse response = new PlayerResponse(11L, 1L, "Scottie", "Pippen", "33", "PIPPEN", now);

        when(playerWebMapper.toCommand(request)).thenReturn(command);
        when(createPlayerUseCase.createPlayer(command)).thenReturn(player);
        when(playerWebMapper.toResponse(player)).thenReturn(response);

        mockMvc.perform(post("/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/players/11"))
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.firstName").value("Scottie"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token is missing on POST")
    void shouldReturn401WhenUnauthenticatedOnPost() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "Michael", "Jordan", "23", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when firstName is blank")
    void shouldReturn400WhenFirstNameIsBlank() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "   ", "Jordan", "23", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.firstName").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when lastName is blank")
    void shouldReturn400WhenLastNameIsBlank() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "Michael", "   ", "23", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.lastName").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when number is blank")
    void shouldReturn400WhenNumberIsBlank() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(1L, "Michael", "Jordan", "   ", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.number").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when teamId is null")
    void shouldReturn400WhenTeamIdIsNull() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(null, "Michael", "Jordan", "23", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.teamId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when teamId is non-positive")
    void shouldReturn400WhenTeamIdIsNonPositive() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(0L, "Michael", "Jordan", "23", "JORDAN");

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.teamId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 Not Found when referenced team does not exist")
    void shouldReturn404WhenReferencedTeamDoesNotExist() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest(999L, "Michael", "Jordan", "23", "JORDAN");
        CreatePlayerCommand command = new CreatePlayerCommand(999L, "Michael", "Jordan", "23", "JORDAN");

        when(playerWebMapper.toCommand(request)).thenReturn(command);
        when(createPlayerUseCase.createPlayer(command)).thenThrow(new TeamNotFoundException(999L));

        mockMvc.perform(post("/api/v1/players")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Team with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 200 OK and all players for GET /api/v1/players")
    void shouldGetAllPlayersOnCanonicalPath() throws Exception {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerResponse r1 = new PlayerResponse(1L, 1L, "Michael", "Jordan", "23", "JORDAN", p1.getCreatedAt());

        when(getPlayerUseCase.findAll()).thenReturn(List.of(p1));
        when(playerWebMapper.toResponseList(List.of(p1))).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/players")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].firstName").value("Michael"));
    }

    @Test
    @DisplayName("Should return 200 OK and all players for GET /players (alias path)")
    void shouldGetAllPlayersOnAliasPath() throws Exception {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerResponse r1 = new PlayerResponse(1L, 1L, "Michael", "Jordan", "23", "JORDAN", p1.getCreatedAt());

        when(getPlayerUseCase.findAll()).thenReturn(List.of(p1));
        when(playerWebMapper.toResponseList(List.of(p1))).thenReturn(List.of(r1));

        mockMvc.perform(get("/players")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Should return 200 OK and players filtered by teamId for GET /api/v1/players?teamId=1")
    void shouldGetPlayersFilteredByTeamId() throws Exception {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerResponse r1 = new PlayerResponse(1L, 1L, "Michael", "Jordan", "23", "JORDAN", p1.getCreatedAt());

        when(getPlayerUseCase.findByTeamId(1L)).thenReturn(List.of(p1));
        when(playerWebMapper.toResponseList(List.of(p1))).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/players?teamId=1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].teamId").value(1L));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when teamId query param is non-positive")
    void shouldReturn400WhenTeamIdQueryParamIsNonPositive() throws Exception {
        mockMvc.perform(get("/api/v1/players?teamId=0")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 200 OK for GET /api/v1/players/{id}")
    void shouldGetPlayerByIdOnCanonicalPath() throws Exception {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerResponse r1 = new PlayerResponse(1L, 1L, "Michael", "Jordan", "23", "JORDAN", p1.getCreatedAt());

        when(getPlayerUseCase.findById(1L)).thenReturn(p1);
        when(playerWebMapper.toResponse(p1)).thenReturn(r1);

        mockMvc.perform(get("/api/v1/players/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("Michael"));
    }

    @Test
    @DisplayName("Should return 200 OK for GET /players/{id} (alias path)")
    void shouldGetPlayerByIdOnAliasPath() throws Exception {
        Player p1 = Player.reconstitute(1L, 1L, "Michael", "Jordan", "23", "JORDAN", Instant.now());
        PlayerResponse r1 = new PlayerResponse(1L, 1L, "Michael", "Jordan", "23", "JORDAN", p1.getCreatedAt());

        when(getPlayerUseCase.findById(1L)).thenReturn(p1);
        when(playerWebMapper.toResponse(p1)).thenReturn(r1);

        mockMvc.perform(get("/players/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Should return 404 Not Found when player does not exist on GET /{id}")
    void shouldReturn404WhenPlayerNotFound() throws Exception {
        when(getPlayerUseCase.findById(999L)).thenThrow(new PlayerNotFoundException(999L));

        mockMvc.perform(get("/api/v1/players/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when id is malformed (alphabetic)")
    void shouldReturn400WhenIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/players/abc")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when id is non-positive")
    void shouldReturn400WhenIdIsNonPositive() throws Exception {
        mockMvc.perform(get("/api/v1/players/0")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for GET /api/v1/players when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for GET /api/v1/players/{id} when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnGetById() throws Exception {
        mockMvc.perform(get("/api/v1/players/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 200 OK and updated representation for PUT /api/v1/players/{id}")
    void shouldUpdatePlayerSuccessfullyOnCanonicalPath() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(2L, "Michael", "Jordan", "45", "COMEBACK");
        UpdatePlayerCommand command = new UpdatePlayerCommand(1L, 2L, "Michael", "Jordan", "45", "COMEBACK");
        Instant now = Instant.now();
        Player updated = Player.reconstitute(1L, 2L, "Michael", "Jordan", "45", "COMEBACK", now);
        PlayerResponse response = new PlayerResponse(1L, 2L, "Michael", "Jordan", "45", "COMEBACK", now);

        when(playerWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updatePlayerUseCase.updatePlayer(command)).thenReturn(updated);
        when(playerWebMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(put("/api/v1/players/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.teamId").value(2L))
                .andExpect(jsonPath("$.number").value("45"))
                .andExpect(jsonPath("$.jerseyName").value("COMEBACK"));
    }

    @Test
    @DisplayName("Should return 200 OK for PUT /players/{id} (alias path)")
    void shouldUpdatePlayerSuccessfullyOnAliasPath() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(1L, "Scottie", "Pippen", "33", "PIPPEN");
        UpdatePlayerCommand command = new UpdatePlayerCommand(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN");
        Instant now = Instant.now();
        Player updated = Player.reconstitute(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN", now);
        PlayerResponse response = new PlayerResponse(2L, 1L, "Scottie", "Pippen", "33", "PIPPEN", now);

        when(playerWebMapper.toCommand(2L, request)).thenReturn(command);
        when(updatePlayerUseCase.updatePlayer(command)).thenReturn(updated);
        when(playerWebMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(put("/players/2")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.firstName").value("Scottie"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for PUT when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnPut() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(1L, "Michael", "Jordan", "23", "JORDAN");

        mockMvc.perform(put("/api/v1/players/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when updating with blank number")
    void shouldReturn400WhenUpdatingWithBlankNumber() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(1L, "Michael", "Jordan", "   ", "JORDAN");

        mockMvc.perform(put("/api/v1/players/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.number").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating non-existent player")
    void shouldReturn404WhenUpdatingNonExistentPlayer() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(1L, "Michael", "Jordan", "23", "JORDAN");
        UpdatePlayerCommand command = new UpdatePlayerCommand(999L, 1L, "Michael", "Jordan", "23", "JORDAN");

        when(playerWebMapper.toCommand(999L, request)).thenReturn(command);
        when(updatePlayerUseCase.updatePlayer(command)).thenThrow(new PlayerNotFoundException(999L));

        mockMvc.perform(put("/api/v1/players/999")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating player with non-existent team")
    void shouldReturn404WhenUpdatingPlayerWithNonExistentTeam() throws Exception {
        UpdatePlayerRequest request = new UpdatePlayerRequest(888L, "Michael", "Jordan", "23", "JORDAN");
        UpdatePlayerCommand command = new UpdatePlayerCommand(1L, 888L, "Michael", "Jordan", "23", "JORDAN");

        when(playerWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updatePlayerUseCase.updatePlayer(command)).thenThrow(new TeamNotFoundException(888L));

        mockMvc.perform(put("/api/v1/players/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Team with ID 888 was not found"));
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /api/v1/players/{id}")
    void shouldDeletePlayerSuccessfullyOnCanonicalPath() throws Exception {
        doNothing().when(deletePlayerUseCase).deletePlayer(1L);

        mockMvc.perform(delete("/api/v1/players/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /players/{id} (alias path)")
    void shouldDeletePlayerSuccessfullyOnAliasPath() throws Exception {
        doNothing().when(deletePlayerUseCase).deletePlayer(2L);

        mockMvc.perform(delete("/players/2")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for DELETE when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/players/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent player")
    void shouldReturn404WhenDeletingNonExistentPlayer() throws Exception {
        doThrow(new PlayerNotFoundException(999L)).when(deletePlayerUseCase).deletePlayer(999L);

        mockMvc.perform(delete("/api/v1/players/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when deleting with malformed ID parameter")
    void shouldReturn400WhenDeletingWithMalformedId() throws Exception {
        mockMvc.perform(delete("/api/v1/players/abc")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when deleting with non-positive ID")
    void shouldReturn400WhenDeletingWithNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/v1/players/0")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}

