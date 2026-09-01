package ctn.informatica.sca.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void accessTokenIncludesUserAndLevel() {
        JwtService jwtService = new JwtService(SECRET, 60, 5);

        String token = jwtService.generateAccessToken(25L, 1, 4);

        assertTrue(jwtService.isValid(token));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isTempToken(token));
        assertEquals(25L, jwtService.extractUserId(token));
        assertEquals(1, jwtService.extractLevel(token));
        assertEquals(4, jwtService.extractSessionVersion(token));
    }

    @Test
    void tempTokenIsMarkedAsTemp() {
        JwtService jwtService = new JwtService(SECRET, 60, 5);

        String token = jwtService.generateTempToken(99L, 3);

        assertTrue(jwtService.isValid(token));
        assertTrue(jwtService.isTempToken(token));
        assertFalse(jwtService.isAccessToken(token));
        assertEquals(99L, jwtService.extractUserId(token));
        assertEquals(3, jwtService.extractLevel(token));
    }
}
