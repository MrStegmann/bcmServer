package com.basketapp.bcmserver.infrastructure.adapter.in.web;

import com.basketapp.bcmserver.application.port.in.CreatePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.DeletePlayerUseCase;
import com.basketapp.bcmserver.application.port.in.GetPlayerUseCase;
import com.basketapp.bcmserver.application.port.in.UpdatePlayerUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.PlayerWebMapper;
import com.basketapp.bcmserver.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PlayerSecurityPrecedenceTest {

    @Autowired
    private MockMvc mockMvc;

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

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/players", "/players"})
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
    @ValueSource(strings = {"/api/v1/players", "/players"})
    @DisplayName("Should reject unauthenticated GET with 401")
    void shouldRejectUnauthenticatedGet(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/players?teamId=-1", "/players?teamId=invalid"})
    @DisplayName("Should reject unauthenticated GET with 401 even when query param is invalid")
    void shouldRejectUnauthenticatedGetWithInvalidQueryParam(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/players/1", "/players/1", "/api/v1/players/-999", "/players/invalid"})
    @DisplayName("Should reject unauthenticated GET by ID with 401 even when ID is invalid")
    void shouldRejectUnauthenticatedGetByIdEvenWithInvalidId(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/players/1", "/players/1", "/api/v1/players/-999", "/players/invalid"})
    @DisplayName("Should reject unauthenticated PUT with 401 even with invalid ID and empty body")
    void shouldRejectUnauthenticatedPutEvenWithInvalidIdAndBody(String path) throws Exception {
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/players/1", "/players/1", "/api/v1/players/-999", "/players/invalid"})
    @DisplayName("Should reject unauthenticated DELETE with 401 even when ID is invalid")
    void shouldRejectUnauthenticatedDeleteEvenWithInvalidId(String path) throws Exception {
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should reject access with invalid bearer token")
    void shouldRejectAccessWithInvalidBearerToken() throws Exception {
        when(tokenProviderPort.validateToken("malformed-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/players/1")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}

