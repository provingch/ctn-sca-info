package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.dto.LoginResponse;
import ctn.informatica.sca.dto.Verify2faRequest;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.security.JwtService;
import ctn.informatica.sca.service.AuthService;
import ctn.informatica.sca.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDao userDao;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDao userDao) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDao = userDao;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            LoginResponse response = authService.login(req);
            maybeIssueRefreshCookie(req.rememberMe(), response.accessToken(), response.level(), httpRequest, httpResponse);
            return ResponseEntity.ok(response);
        } catch (AuthService.AuthException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in /api/auth/login", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir token de sesión extendida", e);
        }
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(@RequestBody Verify2faRequest req, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            LoginResponse response = authService.verify2fa(req);
            maybeIssueRefreshCookie(req.rememberMe(), response.accessToken(), response.level(), httpRequest, httpResponse);
            return ResponseEntity.ok(response);
        } catch (AuthService.AuthException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in /api/auth/2fa/verify", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir token de sesión extendida", e);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String rawToken = readRefreshCookie(request);
            RefreshTokenService.RotationResult rotated = refreshTokenService.rotate(rawToken, request.getHeader("User-Agent"), request.getRemoteAddr());
            if (rotated == null) {
                clearRefreshCookie(response, request.isSecure());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token inválido o expirado");
            }

            User user = userDao.findByIdAndLevel(rotated.userId(), rotated.userLevel());
            if (user == null) {
                clearRefreshCookie(response, request.isSecure());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no válido para refresh token");
            }

            String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel());
            setRefreshCookie(response, rotated.refreshToken(), request.isSecure());
            return ResponseEntity.ok(new RefreshResponse(accessToken, user.getLevel()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al refrescar sesión", ex);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String rawToken = readRefreshCookie(request);
            refreshTokenService.revoke(rawToken);
        } catch (Exception ignored) {
            // no-op: logout should be idempotent
        }
        clearRefreshCookie(response, request.isSecure());
        return ResponseEntity.noContent().build();
    }

    private void maybeIssueRefreshCookie(Boolean rememberMe, String accessToken, Integer level, HttpServletRequest request, HttpServletResponse response) {
        if (!Boolean.TRUE.equals(rememberMe)) {
            return;
        }
        if (accessToken == null || accessToken.isBlank() || level == null) {
            return;
        }

        try {
            Long userId = jwtService.extractUserId(accessToken);
            String refreshToken = refreshTokenService.issueToken(
                    userId.intValue(),
                    level,
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());
            setRefreshCookie(response, refreshToken, request.isSecure());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not issue remember-me refresh cookie; login will continue without persistent session", e);
        }
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (RefreshTokenService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String tokenValue, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, tokenValue)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(RefreshTokenService.REFRESH_TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record RefreshResponse(String accessToken, Integer level) {
    }
}