package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerStatsUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerStatsWebMapper;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerStatsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PlayerStatsSecurityPrecedenceTest {

    @Autowired
    private MockMvc mockMvc;

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

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats", "/player-stats", "/api/v1/player-stats"})
    @DisplayName("Should reject unauthenticated POST with 401 even with completely invalid/empty payload")
    void shouldRejectUnauthenticatedPostEvenWithInvalidPayload(String path) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats", "/player-stats", "/api/v1/player-stats"})
    @DisplayName("Should reject unauthenticated GET collection with 401")
    void shouldRejectUnauthenticatedGetCollection(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should reject unauthenticated GET by ID with 401")
    void shouldRejectUnauthenticatedGetById(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should reject unauthenticated PUT with 401 even with completely invalid/empty payload")
    void shouldRejectUnauthenticatedPutEvenWithInvalidPayload(String path) throws Exception {
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/player-stats/1", "/player-stats/1", "/api/v1/player-stats/1"})
    @DisplayName("Should reject unauthenticated DELETE with 401")
    void shouldRejectUnauthenticatedDelete(String path) throws Exception {
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
