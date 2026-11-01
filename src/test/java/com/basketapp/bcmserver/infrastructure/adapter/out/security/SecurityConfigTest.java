package com.basketapp.bcmserver.infrastructure.adapter.out.security;

import com.basketapp.bcmserver.application.port.in.LoginUserUseCase;
import com.basketapp.bcmserver.application.port.in.RegisterUserUseCase;
import com.basketapp.bcmserver.application.port.out.TokenProviderPort;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.AuthController;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import com.basketapp.bcmserver.infrastructure.adapter.in.web.mapper.UserWebMapper;
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

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private UserWebMapper userWebMapper;

    @MockBean
    private TokenProviderPort tokenProviderPort;

    @Test
    @DisplayName("Should permit unauthenticated access to /api/v1/auth endpoints")
    void shouldPermitAuthEndpointsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isBadRequest()); // reaches controller validation, not rejected by security filter
    }

    @Test
    @DisplayName("Should reject unauthenticated access to protected user endpoints with 401")
    void shouldRejectProtectedEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should reject access with invalid bearer token")
    void shouldRejectAccessWithInvalidBearerToken() throws Exception {
        when(tokenProviderPort.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/teams", "/teams"})
    @DisplayName("Should reject unauthenticated POST to teams endpoints with 401 even when body is completely invalid")
    void shouldRejectUnauthenticatedPostTeamsEvenWithInvalidBody(String path) throws Exception {
        // Empty/invalid payload - security filter must reject with 401 before payload validation occurs
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"category\": \"\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/teams", "/teams"})
    @DisplayName("Should reject unauthenticated GET to teams endpoints with 401")
    void shouldRejectUnauthenticatedGetTeams(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/teams/1", "/teams/1", "/api/v1/teams/-999", "/teams/invalid"})
    @DisplayName("Should reject unauthenticated GET /teams/{id} with 401 even if ID is invalid")
    void shouldRejectUnauthenticatedGetTeamByIdEvenWithInvalidId(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/teams/1", "/teams/1"})
    @DisplayName("Should reject unauthenticated PUT to teams endpoints with 401 even when body is invalid")
    void shouldRejectUnauthenticatedPutTeamsEvenWithInvalidBody(String path) throws Exception {
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/teams/1", "/teams/1", "/api/v1/teams/invalid"})
    @DisplayName("Should reject unauthenticated DELETE to teams endpoints with 401 even if ID is invalid")
    void shouldRejectUnauthenticatedDeleteTeams(String path) throws Exception {
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
