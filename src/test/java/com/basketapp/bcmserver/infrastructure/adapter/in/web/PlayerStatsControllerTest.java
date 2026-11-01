package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsCommand;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerNotFoundException;
import com.basketapp.bcmserver.domain.exception.PlayerStatsNotFoundException;
import com.basketapp.bcmserver.domain.model.PlayerStats;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreatePlayerStatsRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.PlayerStatsResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdatePlayerStatsRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerStatsWebMapper;
import com.basketapp.bcmserver.infrastructure.config.AuthenticatedUser;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(PlayerStatsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PlayerStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePlayerStatsUseCase createPlayerStatsUseCase;

    @MockBean
    private GetPlayerStatsUseCase getPlayerStatsUseCase;

    @MockBean
    private UpdatePlayerStatsUseCase updatePlayerStatsUseCase;

    @MockBean
    private DeletePlayerStatsUseCase deletePlayerStatsUseCase;

    @MockBean
    private PlayerStatsWebMapper playerStatsWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    private UsernamePasswordAuthenticationToken mockAuth() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "coach_john");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats", "/player-stats", "/api/v1/player-stats"})
    @DisplayName("Should return 201 Created and Location header for valid POST on all route aliases")
    void shouldCreatePlayerStatsSuccessfully(String path) throws Exception {
        CreatePlayerStatsRequest request = new CreatePlayerStatsRequest(
                10L, 4L, 28, 45, 6, 5, 10, 7, 5, 2, 2, 6, 8, 3, 2, 3, 24
        );
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                10L, 4L, 28, 45, 6, 5, 10, 7, 5, 2, 2, 6, 8, 3, 2, 3, 24
        );
        PlayerStats stats = PlayerStats.reconstitute(
                1L, 10L, 4L, 28, 45, 6, 5, 10, 7, 5, 2, 2, 6, 8, 3, 2, 3, 24
        );
        PlayerStatsResponse response = new PlayerStatsResponse(
                1L, 10L, 4L, 28, 45, 6, 5, 10, 7, 5, 2, 2, 6, 8, 3, 2, 3, 24
        );

        when(playerStatsWebMapper.toCommand(any(CreatePlayerStatsRequest.class))).thenReturn(command);
        when(createPlayerStatsUseCase.createPlayerStats(any(CreatePlayerStatsCommand.class))).thenReturn(stats);
        when(playerStatsWebMapper.toResponse(any(PlayerStats.class))).thenReturn(response);

        mockMvc.perform(post(path)
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/player-stats/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.game_id").value(10))
                .andExpect(jsonPath("$.player_id").value(4))
                .andExpect(jsonPath("$.minutes").value(28))
                .andExpect(jsonPath("$.seconds").value(45))
                .andExpect(jsonPath("$.valuation").value(24));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when mandatory game_id is missing")
    void shouldReturn400WhenGameIdMissing() throws Exception {
        String payload = """
                {
                    "player_id": 4,
                    "minutes": 20
                }
                """;

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.gameId").value("game_id is required"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when mandatory player_id is missing")
    void shouldReturn400WhenPlayerIdMissing() throws Exception {
        String payload = """
                {
                    "game_id": 10,
                    "minutes": 20
                }
                """;

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.playerId").value("player_id is required"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when seconds is out of bounds (> 59)")
    void shouldReturn400WhenSecondsOutOfBounds() throws Exception {
        String payload = """
                {
                    "game_id": 10,
                    "player_id": 4,
                    "seconds": 65
                }
                """;

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when made shots exceed attempted")
    void shouldReturn400WhenMadeShotsExceedAttempted() throws Exception {
        String payload = """
                {
                    "game_id": 10,
                    "player_id": 4,
                    "t1a": 2,
                    "t1i": 5
                }
                """;

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 404 Not Found when referenced game does not exist")
    void shouldReturn404WhenGameNotFound() throws Exception {
        CreatePlayerStatsRequest request = new CreatePlayerStatsRequest(
                999L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                999L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(playerStatsWebMapper.toCommand(any(CreatePlayerStatsRequest.class))).thenReturn(command);
        when(createPlayerStatsUseCase.createPlayerStats(any(CreatePlayerStatsCommand.class)))
                .thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Game with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when referenced player does not exist")
    void shouldReturn404WhenPlayerNotFound() throws Exception {
        CreatePlayerStatsRequest request = new CreatePlayerStatsRequest(
                10L, 999L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        CreatePlayerStatsCommand command = new CreatePlayerStatsCommand(
                10L, 999L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(playerStatsWebMapper.toCommand(any(CreatePlayerStatsRequest.class))).thenReturn(command);
        when(createPlayerStatsUseCase.createPlayerStats(any(CreatePlayerStatsCommand.class)))
                .thenThrow(new PlayerNotFoundException(999L));

        mockMvc.perform(post("/api/player-stats")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player with ID 999 was not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats", "/player-stats", "/api/v1/player-stats"})
    @DisplayName("Should return 200 OK and list of player stats on all route aliases")
    void shouldReturnAllPlayerStats(String path) throws Exception {
        PlayerStats stats = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStatsResponse response = new PlayerStatsResponse(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        when(getPlayerStatsUseCase.getAllPlayerStats()).thenReturn(List.of(stats));
        when(playerStatsWebMapper.toResponseList(List.of(stats))).thenReturn(List.of(response));

        mockMvc.perform(get(path).with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].game_id").value(10))
                .andExpect(jsonPath("$[0].player_id").value(4));
    }

    @Test
    @DisplayName("Should return 200 OK and filtered stats by gameId")
    void shouldFilterPlayerStatsByGameId() throws Exception {
        PlayerStats stats = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStatsResponse response = new PlayerStatsResponse(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        when(getPlayerStatsUseCase.getPlayerStatsByFilters(10L, null)).thenReturn(List.of(stats));
        when(playerStatsWebMapper.toResponseList(List.of(stats))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/player-stats")
                        .param("gameId", "10")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].game_id").value(10));
    }

    @Test
    @DisplayName("Should return 200 OK and filtered stats by playerId")
    void shouldFilterPlayerStatsByPlayerId() throws Exception {
        PlayerStats stats = PlayerStats.reconstitute(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        PlayerStatsResponse response = new PlayerStatsResponse(1L, 10L, 4L, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        when(getPlayerStatsUseCase.getPlayerStatsByFilters(null, 4L)).thenReturn(List.of(stats));
        when(playerStatsWebMapper.toResponseList(List.of(stats))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/player-stats")
                        .param("playerId", "4")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].player_id").value(4));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when query parameter gameId is non-positive")
    void shouldReturn400WhenInvalidGameIdQueryParam() throws Exception {
        mockMvc.perform(get("/api/player-stats")
                        .param("gameId", "0")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should return 200 OK for GET single player stats on all route aliases")
    void shouldGetSinglePlayerStatsById(String path) throws Exception {
        PlayerStats stats = PlayerStats.reconstitute(1L, 10L, 4L, 25, 30, 2, 2, 4, 3, 1, 1, 2, 4, 5, 1, 1, 2, 18);
        PlayerStatsResponse response = new PlayerStatsResponse(1L, 10L, 4L, 25, 30, 2, 2, 4, 3, 1, 1, 2, 4, 5, 1, 1, 2, 18);

        when(getPlayerStatsUseCase.getPlayerStatsById(1L)).thenReturn(stats);
        when(playerStatsWebMapper.toResponse(stats)).thenReturn(response);

        mockMvc.perform(get(path).with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.game_id").value(10))
                .andExpect(jsonPath("$.player_id").value(4))
                .andExpect(jsonPath("$.minutes").value(25))
                .andExpect(jsonPath("$.valuation").value(18));
    }

    @Test
    @DisplayName("Should return 404 Not Found when single player stats not found")
    void shouldReturn404WhenStatsNotFoundById() throws Exception {
        when(getPlayerStatsUseCase.getPlayerStatsById(999L)).thenThrow(new PlayerStatsNotFoundException(999L));

        mockMvc.perform(get("/api/player-stats/999").with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player stats with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when single player stats ID is non-positive")
    void shouldReturn400WhenInvalidIdPathVariable() throws Exception {
        mockMvc.perform(get("/api/player-stats/0").with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should return 200 OK for valid PUT on all route aliases")
    void shouldUpdatePlayerStatsSuccessfully(String path) throws Exception {
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(
                10L, 4L, 30, 15, 6, 5, 12, 8, 5, 3, 2, 7, 9, 4, 1, 2, 29
        );
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                1L, 10L, 4L, 30, 15, 6, 5, 12, 8, 5, 3, 2, 7, 9, 4, 1, 2, 29
        );
        PlayerStats stats = PlayerStats.reconstitute(
                1L, 10L, 4L, 30, 15, 6, 5, 12, 8, 5, 3, 2, 7, 9, 4, 1, 2, 29
        );
        PlayerStatsResponse response = new PlayerStatsResponse(
                1L, 10L, 4L, 30, 15, 6, 5, 12, 8, 5, 3, 2, 7, 9, 4, 1, 2, 29
        );

        when(playerStatsWebMapper.toCommand(any(Long.class), any(UpdatePlayerStatsRequest.class))).thenReturn(command);
        when(updatePlayerStatsUseCase.updatePlayerStats(any(UpdatePlayerStatsCommand.class))).thenReturn(stats);
        when(playerStatsWebMapper.toResponse(any(PlayerStats.class))).thenReturn(response);

        mockMvc.perform(put(path)
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.game_id").value(10))
                .andExpect(jsonPath("$.player_id").value(4))
                .andExpect(jsonPath("$.minutes").value(30))
                .andExpect(jsonPath("$.valuation").value(29));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when PUT payload is missing mandatory game_id")
    void shouldReturn400WhenPutGameIdMissing() throws Exception {
        String payload = """
                {
                    "player_id": 4,
                    "minutes": 30
                }
                """;

        mockMvc.perform(put("/api/player-stats/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating non-existent stats record")
    void shouldReturn404WhenPutStatsNotFound() throws Exception {
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(
                10L, 4L, 30, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        UpdatePlayerStatsCommand command = new UpdatePlayerStatsCommand(
                999L, 10L, 4L, 30, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(playerStatsWebMapper.toCommand(any(Long.class), any(UpdatePlayerStatsRequest.class))).thenReturn(command);
        when(updatePlayerStatsUseCase.updatePlayerStats(any(UpdatePlayerStatsCommand.class)))
                .thenThrow(new PlayerStatsNotFoundException(999L));

        mockMvc.perform(put("/api/player-stats/999")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player stats with ID 999 was not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should return 204 No Content for DELETE on all route aliases")
    void shouldDeletePlayerStatsSuccessfully(String path) throws Exception {
        doNothing().when(deletePlayerStatsUseCase).deletePlayerStats(1L);

        mockMvc.perform(delete(path).with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent stats record")
    void shouldReturn404WhenDeleteStatsNotFound() throws Exception {
        doThrow(new PlayerStatsNotFoundException(999L)).when(deletePlayerStatsUseCase).deletePlayerStats(999L);

        mockMvc.perform(delete("/api/player-stats/999").with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Player stats with ID 999 was not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when DELETE ID is non-positive")
    void shouldReturn400WhenDeleteIdNonPositive() throws Exception {
        mockMvc.perform(delete("/api/player-stats/0").with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
