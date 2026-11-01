package com.basketapp.bcmserver.infrastructure.adapter.out.ratelimit;

import com.basketapp.bcmserver.infrastructure.adapter.in.web.filter.RateLimitingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimiterAdapterTest {

    private InMemoryRateLimiterAdapter rateLimiter;
    private RateLimitingFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiterAdapter();
        objectMapper = new ObjectMapper();
        filter = new RateLimitingFilter(rateLimiter, objectMapper, 3, 60, 3, 60);
    }

    @Test
    @DisplayName("Should allow requests up to maximum threshold in sliding window")
    void shouldAllowRequestsUpToMax() {
        String key = "test:client:1";
        Duration window = Duration.ofSeconds(10);

        assertTrue(rateLimiter.tryAcquire(key, 3, window));
        assertTrue(rateLimiter.tryAcquire(key, 3, window));
        assertTrue(rateLimiter.tryAcquire(key, 3, window));
        assertFalse(rateLimiter.tryAcquire(key, 3, window));
    }

    @Test
    @DisplayName("Should reset counter when reset is invoked")
    void shouldResetCounter() {
        String key = "test:client:2";
        Duration window = Duration.ofSeconds(10);

        assertTrue(rateLimiter.tryAcquire(key, 1, window));
        assertFalse(rateLimiter.tryAcquire(key, 1, window));

        rateLimiter.reset(key);
        assertTrue(rateLimiter.tryAcquire(key, 1, window));
    }

    @Test
    @DisplayName("RateLimitingFilter should pass unthrottled request through filter chain")
    void shouldPassUnthrottledRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus()); // default MockHttpServletResponse status is 200
    }

    @Test
    @DisplayName("RateLimitingFilter should return 429 with Retry-After when rate limit is exceeded")
    void shouldReturn429WhenRateLimitExceeded() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("10.0.0.5");
        FilterChain filterChain = mock(FilterChain.class);

        // Exhaust the 3 allowed requests
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        // 4th request must be rejected with 429
        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilter(request, rejectedResponse, filterChain);

        assertEquals(429, rejectedResponse.getStatus());
        assertEquals("60", rejectedResponse.getHeader("Retry-After"));
        assertTrue(rejectedResponse.getContentType().contains("application/problem+json"));
        assertTrue(rejectedResponse.getContentAsString().contains("Rate limit exceeded"));
    }
}

