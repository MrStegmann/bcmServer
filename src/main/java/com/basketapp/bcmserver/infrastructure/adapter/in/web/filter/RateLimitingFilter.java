package com.basketapp.bcmserver.infrastructure.adapter.in.web.filter;

import com.basketapp.bcmserver.application.port.out.RateLimiterPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * Filter intercepting authentication routes to enforce IP-based rate limiting
 * and emit standard RFC 7807 429 Too Many Requests responses with Retry-After header.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterPort rateLimiter;
    private final ObjectMapper objectMapper;
    private final int registrationMax;
    private final int registrationWindowSeconds;
    private final int loginMax;
    private final int loginWindowSeconds;

    public RateLimitingFilter(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RateLimiterPort rateLimiter,
            ObjectMapper objectMapper,
            @Value("${security.rate-limit.registration.max-requests:5}") int registrationMax,
            @Value("${security.rate-limit.registration.window-seconds:60}") int registrationWindowSeconds,
            @Value("${security.rate-limit.login.max-requests:5}") int loginMax,
            @Value("${security.rate-limit.login.window-seconds:60}") int loginWindowSeconds
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.registrationMax = registrationMax;
        this.registrationWindowSeconds = registrationWindowSeconds;
        this.loginMax = loginMax;
        this.loginWindowSeconds = loginWindowSeconds;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (rateLimiter != null && "POST".equalsIgnoreCase(method)) {
            if (uri.endsWith("/api/v1/auth/register")) {
                String clientIp = resolveClientIp(request);
                String key = "register:" + clientIp;
                if (!rateLimiter.tryAcquire(key, registrationMax, Duration.ofSeconds(registrationWindowSeconds))) {
                    rejectWithRateLimitProblem(request, response, registrationWindowSeconds);
                    return;
                }
            } else if (uri.endsWith("/api/v1/auth/login")) {
                String clientIp = resolveClientIp(request);
                String key = "login:" + clientIp;
                if (!rateLimiter.tryAcquire(key, loginMax, Duration.ofSeconds(loginWindowSeconds))) {
                    rejectWithRateLimitProblem(request, response, loginWindowSeconds);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void rejectWithRateLimitProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            int retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Please try again later."
        );
        problem.setTitle("Too Many Requests");
        problem.setInstance(URI.create(request.getRequestURI()));

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
