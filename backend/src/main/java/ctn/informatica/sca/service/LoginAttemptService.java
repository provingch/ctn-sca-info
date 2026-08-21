package ctn.informatica.sca.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES_BEFORE_LOCK = 5;
    private static final Duration[] LOCK_DURATIONS = {
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30)
    };

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public synchronized long blockedSeconds(String... keys) {
        long remaining = 0;
        Instant now = Instant.now();
        for (String key : keys) {
            AttemptState state = attempts.get(key);
            if (state == null) {
                continue;
            }
            if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
                remaining = Math.max(remaining, secondsUntil(state.lockedUntil, now));
            } else if (state.lockedUntil != null) {
                state.lockedUntil = null;
            }
        }
        return remaining;
    }

    public synchronized long recordFailure(String... keys) {
        long remaining = 0;
        Instant now = Instant.now();
        for (String key : keys) {
            AttemptState state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
            state.failures++;
            if (state.failures >= MAX_FAILURES_BEFORE_LOCK) {
                int lockIndex = Math.min(state.failures - MAX_FAILURES_BEFORE_LOCK, LOCK_DURATIONS.length - 1);
                state.lockedUntil = now.plus(LOCK_DURATIONS[lockIndex]);
                remaining = Math.max(remaining, secondsUntil(state.lockedUntil, now));
            }
        }
        return remaining;
    }

    public synchronized void clear(String... keys) {
        for (String key : keys) {
            attempts.remove(key);
        }
    }

    private long secondsUntil(Instant deadline, Instant now) {
        return Math.max(1, Duration.between(now, deadline).toSeconds());
    }

    private static final class AttemptState {
        private int failures;
        private Instant lockedUntil;
    }
}