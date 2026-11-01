package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreateGameCommand;
import com.basketapp.bcmserver.application.port.in.CreateGameUseCase;
import com.basketapp.bcmserver.application.port.in.DeleteGameUseCase;
import com.basketapp.bcmserver.application.port.in.GetGameUseCase;
import com.basketapp.bcmserver.application.port.in.UpdateGameCommand;
import com.basketapp.bcmserver.application.port.in.UpdateGameUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.domain.exception.GameNotFoundException;
import com.basketapp.bcmserver.domain.exception.TeamNotFoundException;
import com.basketapp.bcmserver.domain.model.Game;
import com.basketapp.bcmserver.domain.model.GameStatus;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.CreateGameRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.GameResponse;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.dto.UpdateGameRequest;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.GameWebMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateGameUseCase createGameUseCase;

    @MockBean
    private GetGameUseCase getGameUseCase;

    @MockBean
    private UpdateGameUseCase updateGameUseCase;

    @MockBean
    private DeleteGameUseCase deleteGameUseCase;

    @MockBean
    private GameWebMapper gameWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    private UsernamePasswordAuthenticationToken mockAuth() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "coach_john");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should return 201 Created and Location header for POST /api/games")
    void shouldCreateGameSuccessfullyOnCanonicalPath() throws Exception {
        CreateGameRequest request = new CreateGameRequest(
                1L, "Lakers", "1", "2026-11-01", "pending",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );
        CreateGameCommand command = new CreateGameCommand(
                1L, "Lakers", "1", "2026-11-01", "pending",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                10L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, now,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );
        GameResponse response = new GameResponse(
                10L, 1L, "Lakers", "1", "2026-11-01", "pending", now,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0
        );

        when(gameWebMapper.toCommand(request)).thenReturn(command);
        when(createGameUseCase.createGame(command)).thenReturn(game);
        when(gameWebMapper.toResponse(game)).thenReturn(response);

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/games/10"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.team_id").value(1L))
                .andExpect(jsonPath("$.opponent").value("Lakers"))
                .andExpect(jsonPath("$.round").value("1"))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    @DisplayName("Should return 201 Created for POST /games (alias path)")
    void shouldCreateGameSuccessfullyOnAliasPath() throws Exception {
        CreateGameRequest request = new CreateGameRequest(
                1L, "Bulls", "1", "2026-11-02", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );
        CreateGameCommand command = new CreateGameCommand(
                1L, "Bulls", "1", "2026-11-02", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                11L, 1L, "Bulls", "1", "2026-11-02", GameStatus.PENDING, now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        GameResponse response = new GameResponse(
                11L, 1L, "Bulls", "1", "2026-11-02", "pending", now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameWebMapper.toCommand(request)).thenReturn(command);
        when(createGameUseCase.createGame(command)).thenReturn(game);
        when(gameWebMapper.toResponse(game)).thenReturn(response);

        mockMvc.perform(post("/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/games/11"))
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.opponent").value("Bulls"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when team_id is missing")
    void shouldReturn400WhenTeamIdMissing() throws Exception {
        String jsonPayload = """
                {
                    "opponent": "Lakers",
                    "round": "1",
                    "date": "2026-11-01"
                }
                """;

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.teamId").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when opponent is blank")
    void shouldReturn400WhenOpponentBlank() throws Exception {
        String jsonPayload = """
                {
                    "team_id": 1,
                    "opponent": "   ",
                    "round": "1",
                    "date": "2026-11-01"
                }
                """;

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.opponent").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when date is blank")
    void shouldReturn400WhenDateBlank() throws Exception {
        String jsonPayload = """
                {
                    "team_id": 1,
                    "opponent": "Lakers",
                    "date": "   "
                }
                """;

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.date").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when status is invalid")
    void shouldReturn400WhenStatusInvalid() throws Exception {
        String jsonPayload = """
                {
                    "team_id": 1,
                    "opponent": "Lakers",
                    "date": "2026-11-01",
                    "status": "invalid_status"
                }
                """;

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.status").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when score is negative")
    void shouldReturn400WhenScoreNegative() throws Exception {
        String jsonPayload = """
                {
                    "team_id": 1,
                    "opponent": "Lakers",
                    "date": "2026-11-01",
                    "result_c1": -5
                }
                """;

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.resultC1").exists());
    }

    @Test
    @DisplayName("Should return 404 Not Found when TeamNotFoundException is thrown")
    void shouldReturn404WhenTeamNotFound() throws Exception {
        CreateGameRequest request = new CreateGameRequest(
                999L, "Lakers", "1", "2026-11-01", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );
        CreateGameCommand command = new CreateGameCommand(
                999L, "Lakers", "1", "2026-11-01", "pending",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );

        when(gameWebMapper.toCommand(request)).thenReturn(command);
        when(createGameUseCase.createGame(command)).thenThrow(new TeamNotFoundException(999L));

        mockMvc.perform(post("/api/games")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 200 OK and list of games for GET /api/games")
    void shouldReturnAllGames() throws Exception {
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        GameResponse response = new GameResponse(
                1L, 1L, "Lakers", "1", "2026-11-01", "pending", now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(getGameUseCase.findAll()).thenReturn(List.of(game));
        when(gameWebMapper.toResponseList(List.of(game))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/games")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].opponent").value("Lakers"));
    }

    @Test
    @DisplayName("Should return 200 OK and filtered games for GET /api/games?teamId=1")
    void shouldReturnFilteredGamesByTeamId() throws Exception {
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                1L, 1L, "Lakers", "1", "2026-11-01", GameStatus.PENDING, now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        GameResponse response = new GameResponse(
                1L, 1L, "Lakers", "1", "2026-11-01", "pending", now,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(getGameUseCase.findByTeamId(1L)).thenReturn(List.of(game));
        when(gameWebMapper.toResponseList(List.of(game))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/games")
                        .param("teamId", "1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].team_id").value(1L));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when teamId query param is non-positive")
    void shouldReturn400WhenTeamIdQueryParamNonPositive() throws Exception {
        mockMvc.perform(get("/api/games")
                        .param("teamId", "-1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 200 OK and single game for GET /api/games/{id}")
    void shouldReturnSingleGameById() throws Exception {
        Instant now = Instant.now();
        Game game = Game.reconstitute(
                5L, 2L, "Celtics", "Finals", "2026-11-10", GameStatus.LIVE, now,
                25, 20, 22, 28, 0,
                18, 24, 21, 26, 0,
                4, 3, 5, 2, 0,
                3, 4, 2, 5, 0
        );
        GameResponse response = new GameResponse(
                5L, 2L, "Celtics", "Finals", "2026-11-10", "live", now,
                25, 20, 22, 28, 0,
                18, 24, 21, 26, 0,
                4, 3, 5, 2, 0,
                3, 4, 2, 5, 0
        );

        when(getGameUseCase.findById(5L)).thenReturn(game);
        when(gameWebMapper.toResponse(game)).thenReturn(response);

        mockMvc.perform(get("/api/games/5")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.opponent").value("Celtics"))
                .andExpect(jsonPath("$.result_c1").value(25))
                .andExpect(jsonPath("$.fouls_c1").value(4));
    }

    @Test
    @DisplayName("Should return 404 Not Found when game not found for GET /api/games/{id}")
    void shouldReturn404WhenGameNotFoundById() throws Exception {
        when(getGameUseCase.findById(999L)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/games/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when path variable ID is non-positive")
    void shouldReturn400WhenPathVariableIdNonPositive() throws Exception {
        mockMvc.perform(get("/api/games/-1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 200 OK when updating game successfully")
    void shouldUpdateGameSuccessfully() throws Exception {
        UpdateGameRequest request = new UpdateGameRequest(
                1L, "Clippers", "2", "2026-11-05", "live",
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0
        );
        UpdateGameCommand command = new UpdateGameCommand(
                1L, 1L, "Clippers", "2", "2026-11-05", "live",
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0
        );
        Instant now = Instant.now();
        Game updatedGame = Game.reconstitute(
                1L, 1L, "Clippers", "2", "2026-11-05", GameStatus.LIVE, now,
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0
        );
        GameResponse response = new GameResponse(
                1L, 1L, "Clippers", "2", "2026-11-05", "live", now,
                20, 22, 24, 26, 0,
                18, 19, 21, 25, 0,
                3, 2, 4, 5, 0,
                2, 3, 4, 1, 0
        );

        when(gameWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updateGameUseCase.updateGame(command)).thenReturn(updatedGame);
        when(gameWebMapper.toResponse(updatedGame)).thenReturn(response);

        mockMvc.perform(put("/api/games/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.opponent").value("Clippers"))
                .andExpect(jsonPath("$.status").value("live"))
                .andExpect(jsonPath("$.result_c1").value(20));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when updating game with non-positive ID")
    void shouldReturn400WhenUpdatingGameWithNonPositiveId() throws Exception {
        UpdateGameRequest request = new UpdateGameRequest(
                1L, "Clippers", "2", "2026-11-05", "live",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        mockMvc.perform(put("/api/games/-1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when updating game with missing scores")
    void shouldReturn400WhenUpdatingGameWithMissingScores() throws Exception {
        String jsonPayload = """
                {
                    "team_id": 1,
                    "opponent": "Clippers",
                    "round": "2",
                    "date": "2026-11-05",
                    "status": "live"
                }
                """;

        mockMvc.perform(put("/api/games/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.resultC1").exists());
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating non-existent game")
    void shouldReturn404WhenUpdatingNonExistentGame() throws Exception {
        UpdateGameRequest request = new UpdateGameRequest(
                1L, "Clippers", "2", "2026-11-05", "live",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        UpdateGameCommand command = new UpdateGameCommand(
                999L, 1L, "Clippers", "2", "2026-11-05", "live",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameWebMapper.toCommand(999L, request)).thenReturn(command);
        when(updateGameUseCase.updateGame(command)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(put("/api/games/999")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating game with non-existent team")
    void shouldReturn404WhenUpdatingGameWithNonExistentTeam() throws Exception {
        UpdateGameRequest request = new UpdateGameRequest(
                999L, "Clippers", "2", "2026-11-05", "live",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        UpdateGameCommand command = new UpdateGameCommand(
                1L, 999L, "Clippers", "2", "2026-11-05", "live",
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        when(gameWebMapper.toCommand(1L, request)).thenReturn(command);
        when(updateGameUseCase.updateGame(command)).thenThrow(new TeamNotFoundException(999L));

        mockMvc.perform(put("/api/games/1")
                        .with(authentication(mockAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 204 No Content for DELETE /api/games/{id}")
    void shouldDeleteGameSuccessfully() throws Exception {
        doNothing().when(deleteGameUseCase).deleteGame(1L);

        mockMvc.perform(delete("/api/games/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 204 No Content for alias DELETE /games/{id}")
    void shouldDeleteGameSuccessfullyOnAliasPath() throws Exception {
        doNothing().when(deleteGameUseCase).deleteGame(2L);

        mockMvc.perform(delete("/games/2")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when deleting game with non-positive ID")
    void shouldReturn400WhenDeletingGameWithNonPositiveId() throws Exception {
        mockMvc.perform(delete("/api/games/-1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent game")
    void shouldReturn404WhenDeletingNonExistentGame() throws Exception {
        doThrow(new GameNotFoundException(999L)).when(deleteGameUseCase).deleteGame(999L);

        mockMvc.perform(delete("/api/games/999")
                        .with(authentication(mockAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
