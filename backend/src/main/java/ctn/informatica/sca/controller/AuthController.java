package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.dto.LoginResponse;
import ctn.informatica.sca.dto.Verify2faRequest;
import ctn.informatica.sca.dto.AuthErrorResponse;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.security.JwtService;
import ctn.informatica.sca.service.ActivityLogService;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ActivityLogService activityLogService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDao userDao) {
        this(authService, jwtService, refreshTokenService, userDao, null);
    }

    @Autowired
    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDao userDao,
            ActivityLogService activityLogService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDao = userDao;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            LoginResponse response = authService.login(req, httpRequest.getRemoteAddr());
            issueRefreshCookie(req.rememberMe(), response.accessToken(), response.level(), httpRequest, httpResponse);
            try {
                if (activityLogService != null && response != null && response.accessToken() != null && !response.accessToken().isBlank()) {
                    int userId = jwtService.extractUserId(response.accessToken()).intValue();
                    activityLogService.registrar(userId, "Inició sesión");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "No se pudo registrar el login para el usuario", ex);
            }
            return ResponseEntity.ok(response);
        } catch (AuthService.AuthException e) {
            ResponseEntity.BodyBuilder response = ResponseEntity.status(e.status());
            if (e.status() == 429) response.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()));
            return response.body(new AuthErrorResponse(
                e.status() == 429 ? "AUTH_LOCKED" : "AUTH_FAILED", e.getMessage(), e.retryAfterSeconds()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in /api/auth/login", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir token de sesión extendida", e);
        }
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(@RequestBody Verify2faRequest req, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            LoginResponse response = authService.verify2fa(req, httpRequest.getRemoteAddr());
            issueRefreshCookie(req.rememberMe(), response.accessToken(), response.level(), httpRequest, httpResponse);
            try {
                if (activityLogService != null && response != null && response.accessToken() != null && !response.accessToken().isBlank()) {
                    int userId = jwtService.extractUserId(response.accessToken()).intValue();
                    activityLogService.registrar(userId, "Inició sesión");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "No se pudo registrar el login para el usuario: {0}", ex.getMessage());
            }
            return ResponseEntity.ok(response);
        } catch (AuthService.AuthException e) {
            ResponseEntity.BodyBuilder response = ResponseEntity.status(e.status());
            if (e.status() == 429) response.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()));
            return response.body(new AuthErrorResponse(
                e.status() == 429 ? "AUTH_LOCKED" : "AUTH_FAILED", e.getMessage(), e.retryAfterSeconds()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in /api/auth/2fa/verify", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir token de sesión extendida", e);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            RefreshCookie refreshCookie = readRefreshCookie(request);
            String rawToken = refreshCookie != null ? refreshCookie.value() : null;
            RefreshTokenService.RotationResult rotated = refreshTokenService.rotate(rawToken, request.getHeader("User-Agent"), request.getRemoteAddr());
            if (rotated == null) {
                clearRefreshCookies(response, request.isSecure());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token inválido o expirado");
            }

            User user = userDao.findByIdAndLevel(rotated.userId(), rotated.userLevel());
            if (user == null) {
                clearRefreshCookies(response, request.isSecure());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no válido para refresh token");
            }

            String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel());
            setRefreshCookie(response, rotated.refreshToken(), request.isSecure(), refreshCookie.persistent());
            return ResponseEntity.ok(new RefreshResponse(accessToken, user.getLevel()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error en /api/auth/refresh", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al refrescar sesión", ex);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            refreshTokenService.revoke(readCookie(request, RefreshTokenService.COOKIE_NAME));
            refreshTokenService.revoke(readCookie(request, RefreshTokenService.SESSION_COOKIE_NAME));
        } catch (Exception ignored) {
            // no-op: logout should be idempotent
        }
        clearRefreshCookies(response, request.isSecure());
        return ResponseEntity.noContent().build();
    }

    private void issueRefreshCookie(Boolean rememberMe, String accessToken, Integer level, HttpServletRequest request, HttpServletResponse response) {
        if (accessToken == null || accessToken.isBlank() || level == null) {
            return;
        }

        try {
            boolean persistent = Boolean.TRUE.equals(rememberMe);
            Long userId = jwtService.extractUserId(accessToken);
            String refreshToken = refreshTokenService.issueToken(
                    userId.intValue(),
                    level,
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());
            clearRefreshCookie(
                    response,
                    request.isSecure(),
                    persistent ? RefreshTokenService.SESSION_COOKIE_NAME : RefreshTokenService.COOKIE_NAME);
            setRefreshCookie(response, refreshToken, request.isSecure(), persistent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Login succeeded but refresh cookie could not be persisted. Returning access token without persistent session state.", e);
        }
    }

    private RefreshCookie readRefreshCookie(HttpServletRequest request) {
        String persistentToken = readCookie(request, RefreshTokenService.COOKIE_NAME);
        if (persistentToken != null && !persistentToken.isBlank()) {
            return new RefreshCookie(persistentToken, true);
        }
        String sessionToken = readCookie(request, RefreshTokenService.SESSION_COOKIE_NAME);
        return sessionToken == null || sessionToken.isBlank() ? null : new RefreshCookie(sessionToken, false);
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String tokenValue, boolean secure, boolean persistent) {
        String cookieName = persistent ? RefreshTokenService.COOKIE_NAME : RefreshTokenService.SESSION_COOKIE_NAME;
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, tokenValue)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");
        if (persistent) {
            builder.maxAge(RefreshTokenService.REFRESH_TTL);
        }
        ResponseCookie cookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookies(HttpServletResponse response, boolean secure) {
        clearRefreshCookie(response, secure, RefreshTokenService.COOKIE_NAME);
        clearRefreshCookie(response, secure, RefreshTokenService.SESSION_COOKIE_NAME);
    }

    private void clearRefreshCookie(HttpServletResponse response, boolean secure, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
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

    private record RefreshCookie(String value, boolean persistent) {
    }
}
