package com.basketapp.bcmserver.infrastructure.adapter.out.ratelimit;

import com.basketapp.bcmserver.application.port.out.RateLimiterPort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory sliding window rate limiter adapter.
 */
@Component
public class InMemoryRateLimiterAdapter implements RateLimiterPort {

    private final ConcurrentHashMap<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        if (key == null || maxRequests <= 0 || window == null || window.isZero() || window.isNegative()) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        long threshold = now - windowMillis;

        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }

    @Override
    public void reset(String key) {
        if (key != null) {
            requestLogs.remove(key);
        }
    }

    public void clear() {
        requestLogs.clear();
    }
}

