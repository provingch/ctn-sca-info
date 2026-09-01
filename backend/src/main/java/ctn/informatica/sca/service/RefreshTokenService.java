package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.RefreshTokenDao;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    public static final String COOKIE_NAME = "SCA_REMEMBER";
    public static final String SESSION_COOKIE_NAME = "SCA_SESSION";
    public static final Duration REFRESH_TTL = Duration.ofDays(30);

    private final RefreshTokenDao refreshTokenDao;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenDao refreshTokenDao) {
        this.refreshTokenDao = refreshTokenDao;
    }

    public String issueToken(int userId, int userLevel, String userAgent, String ipAddress) throws Exception {
        String raw = generateOpaqueToken();
        String hash = sha256Hex(raw);
        Instant expiresAt = Instant.now().plus(REFRESH_TTL);
        refreshTokenDao.insert(hash, userId, userLevel, expiresAt, userAgent, ipAddress);
        return raw;
    }

    public RotationResult rotate(String rawToken, String userAgent, String ipAddress) throws Exception {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String oldHash = sha256Hex(rawToken.trim());
        RefreshTokenDao.RefreshTokenRecord record = refreshTokenDao.findActiveByHash(oldHash);
        if (record == null) {
            return null;
        }

        String newRaw = generateOpaqueToken();
        String newHash = sha256Hex(newRaw);
        Instant newExpiresAt = Instant.now().plus(REFRESH_TTL);
        boolean rotated = refreshTokenDao.rotate(oldHash, newHash, newExpiresAt, userAgent, ipAddress);
        if (!rotated) {
            return null;
        }

        return new RotationResult(newRaw, record.userId(), record.userLevel());
    }

    public void revoke(String rawToken) throws Exception {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = sha256Hex(rawToken.trim());
        refreshTokenDao.revoke(hash);
    }

    public int revokeAllForUser(int userId) throws SQLException {
        return refreshTokenDao.revokeAllByUserId(userId);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record RotationResult(String refreshToken, int userId, int userLevel) {
    }
}
