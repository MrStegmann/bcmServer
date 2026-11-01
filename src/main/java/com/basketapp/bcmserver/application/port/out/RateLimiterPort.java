package com.basketapp.bcmserver.application.port.out;

import java.time.Duration;

/**
 * Outbound port for request rate limiting and abuse prevention.
 */
public interface RateLimiterPort {

    /**
     * Attempts to acquire permission for an event under the given key.
     *
     * @param key unique identifier (e.g. client IP or IP + username)
     * @param maxRequests maximum allowed requests within the window
     * @param window time window duration
     * @return true if request is permitted, false if rate limit is exceeded
     */
    boolean tryAcquire(String key, int maxRequests, Duration window);

    /**
     * Resets or clears rate limit counter for a specific key.
     *
     * @param key unique identifier
     */
    void reset(String key);
}

