package ctn.informatica.sia.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class RememberMeTokenStore {

    private static final long REMEMBER_TTL_MILLIS = TimeUnit.DAYS.toMillis(30);
    private static final Map<String, TokenEntry> TOKENS = new ConcurrentHashMap<>();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RememberMeTokenStore() {
    }

    public static String issueToken(int userId) {
        String token = generateToken();
        TOKENS.put(token, new TokenEntry(userId, System.currentTimeMillis() + REMEMBER_TTL_MILLIS));
        return token;
    }

    public static Optional<Integer> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        TokenEntry entry = TOKENS.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() > entry.expiresAtMillis) {
            TOKENS.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.userId);
    }

    public static void invalidateToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        TOKENS.remove(token);
    }

    public static void invalidateUserTokens(int userId) {
        TOKENS.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue().userId == userId);
    }

    private static String generateToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class TokenEntry {
        private final int userId;
        private final long expiresAtMillis;

        private TokenEntry(int userId, long expiresAtMillis) {
            this.userId = userId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
