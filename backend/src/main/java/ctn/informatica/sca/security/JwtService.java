package ctn.informatica.sca.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;
    private final long tempTokenMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${jwt.temp-token-minutes}") long tempTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenMinutes = accessTokenMinutes;
        this.tempTokenMinutes = tempTokenMinutes;
    }

    /** Token definitivo, ya autenticado (post 2FA si aplica). */
    public String generateAccessToken(Long userId, int level) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenMinutes * 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("level", level)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** Token intermedio, solo válido para completar el paso de 2FA. */
    public String generateTempToken(Long userId, int level) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tempTokenMinutes * 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("level", level)
                .claim("type", "temp")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Integer extractLevel(String token) {
        return parseClaims(token).get("level", Integer.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).get("type", String.class));
    }

    public boolean isTempToken(String token) {
        return "temp".equals(parseClaims(token).get("type", String.class));
    }

    /** true si el token es válido (firma correcta y no expirado). */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}