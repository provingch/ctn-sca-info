package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.dto.LoginResponse;
import ctn.informatica.sca.dto.Verify2faRequest;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.security.JwtService;
import ctn.informatica.sca.util.TotpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PadreDao padreDao;
    private final ProfesorDao profesorDao;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserDao userDao, PadreDao padreDao, ProfesorDao profesorDao, JwtService jwtService,
            LoginAttemptService loginAttemptService) {
        this.userDao = userDao;
        this.padreDao = padreDao;
        this.profesorDao = profesorDao;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public static class AuthException extends RuntimeException {
        private final int status;
        private final long retryAfterSeconds;

        public AuthException(String message) { this(message, 401, 0); }

        public AuthException(String message, int status, long retryAfterSeconds) {
            super(message);
            this.status = status;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public int status() { return status; }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }

    public LoginResponse login(LoginRequest req) {
        return login(req, "unknown");
    }

    public LoginResponse login(LoginRequest req, String ipAddress) {
        String usernameKey = usernameKey(req.username(), ipAddress);
        String ipKey = ipKey(ipAddress);
        rejectIfBlocked(usernameKey, ipKey);
        User user;
        try {
            user = userDao.findByUsernameAndPassword(req.username(), req.password());
        } catch (Exception e) {
            log.error("[AUTH] Error de base de datos durante login para usuario {}", req.username(), e);
            throw new RuntimeException("Error de base de datos durante login", e);
        }
        if (user == null) {
            rejectAfterFailure(usernameKey, ipKey, "Credenciales inválidas");
        }

        String secret = getTotpSecret(user);

        if (secret != null && !secret.isBlank()) {
            loginAttemptService.clear(usernameKey, ipKey);
            String tempToken = jwtService.generateTempToken((long) user.getId(), user.getLevel());
            return new LoginResponse(true, tempToken, null, null);
        }

        try {
            loginAttemptService.clear(usernameKey, ipKey);
            String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel(), user.getSessionVersion());
            return new LoginResponse(false, null, accessToken, user.getLevel());
        } catch (Exception e) {
            log.error("[AUTH] Error generando access token para usuario {}", user.getId(), e);
            throw e;
        }
    }

    public LoginResponse verify2fa(Verify2faRequest req) {
        return verify2fa(req, "unknown");
    }

    public LoginResponse verify2fa(Verify2faRequest req, String ipAddress) {
        if (!jwtService.isValid(req.tempToken()) || !jwtService.isTempToken(req.tempToken())) {
            rejectAfterFailure(ipKey(ipAddress), "Token temporal inválido o expirado");
        }
        Long userId = jwtService.extractUserId(req.tempToken());
        String userKey = userKey(userId, ipAddress);
        String ipKey = ipKey(ipAddress);
        rejectIfBlocked(userKey, ipKey);

        User user;
        try {
            user = userDao.findById(userId.intValue());
        } catch (Exception e) {
            log.error("[AUTH] Error de base de datos durante verify2fa para userId {}", userId, e);
            throw new RuntimeException("Error de base de datos durante verify2fa", e);
        }
        if (user == null) {
            rejectAfterFailure(userKey, ipKey, "Código de autenticación inválido");
        }

        String secret = getTotpSecret(user);
        if (!TotpUtils.verifyCode(secret, req.code())) {
            rejectAfterFailure(userKey, ipKey, "Código de autenticación inválido");
        }

        try {
            loginAttemptService.clear(userKey, ipKey);
            String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel(), user.getSessionVersion());
            return new LoginResponse(false, null, accessToken, user.getLevel());
        } catch (Exception e) {
            log.error("[AUTH] Error generando access token para usuario {}", user.getId(), e);
            throw e;
        }
    }

    private void rejectIfBlocked(String... keys) {
        long remaining = loginAttemptService.blockedSeconds(keys);
        if (remaining > 0) {
            throw new AuthException("Demasiados intentos. Volvé a intentar en " + formatDuration(remaining) + ".",
                    429, remaining);
        }
    }

    private void rejectAfterFailure(String key, String message) {
        rejectAfterFailure(new String[] { key }, message);
    }

    private void rejectAfterFailure(String key1, String key2, String message) {
        rejectAfterFailure(new String[] { key1, key2 }, message);
    }

    private void rejectAfterFailure(String[] keys, String message) {
        long remaining = loginAttemptService.recordFailure(keys);
        if (remaining > 0) {
            throw new AuthException("Demasiados intentos. Volvé a intentar en " + formatDuration(remaining) + ".",
                    429, remaining);
        }
        throw new AuthException(message);
    }

    private String usernameKey(String username, String ipAddress) {
        return "username:" + normalize(username) + "|ip:" + normalizeIp(ipAddress);
    }

    private String userKey(long userId, String ipAddress) {
        return "user:" + userId + "|ip:" + normalizeIp(ipAddress);
    }

    private String ipKey(String ipAddress) {
        return "ip:" + normalizeIp(ipAddress);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + " segundos";
        long minutes = (seconds + 59) / 60;
        return minutes + (minutes == 1 ? " minuto" : " minutos");
    }

    // Réplica exacta de la regla en LoginServlet/TotpServlet: nivel 4 (padre) busca
    // el secreto en PadreDao, cualquier otro nivel lo busca en ProfesorDao.
    private String getTotpSecret(User user) {
        try {
            if (user.getLevel() == 4) {
                var padre = padreDao.findById(user.getId());
                if (padre == null) {
                    throw new IllegalStateException("No existe el perfil familiar asociado");
                }
                return padre.getTotpSecret();
            } else {
                var profesor = profesorDao.findById(user.getId());
                if (profesor == null) {
                    throw new IllegalStateException("No existe el perfil institucional asociado");
                }
                return profesor.getTotpSecret();
            }
        } catch (Exception e) {
            log.error("[AUTH] No se pudo verificar la configuración 2FA del usuario {}", user.getId(), e);
            throw new AuthException("No se pudo validar la configuración de seguridad. Intentá nuevamente más tarde.", 503, 0);
        }
    }
}
