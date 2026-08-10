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
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PadreDao padreDao;
    private final ProfesorDao profesorDao;
    private final JwtService jwtService;

    public AuthService(UserDao userDao, PadreDao padreDao, ProfesorDao profesorDao, JwtService jwtService) {
        this.userDao = userDao;
        this.padreDao = padreDao;
        this.profesorDao = profesorDao;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    }

    public static class AuthException extends RuntimeException {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);

        public AuthException(String message) { super(message); }
    }

    public LoginResponse login(LoginRequest req) {
        User user;
        try {
            user = userDao.findByUsernameAndPassword(req.username(), req.password());
        } catch (Exception e) {
            throw new RuntimeException("Error de base de datos durante login", e);
        }
        if (user == null) {
            throw new AuthException("Credenciales inválidas");
        }

        String secret = getTotpSecret(user);

        if (secret != null && !secret.isBlank()) {
            String tempToken = jwtService.generateTempToken((long) user.getId(), user.getLevel());
            return new LoginResponse(true, tempToken, null, null);
        }

                log.error("[AUTH] Error de base de datos durante login para usuario {}", req.username(), e);
                throw new RuntimeException("Error de base de datos durante login", e);
        return new LoginResponse(false, null, accessToken, user.getLevel());
    }

    public LoginResponse verify2fa(Verify2faRequest req) {
        if (!jwtService.isValid(req.tempToken()) || !jwtService.isTempToken(req.tempToken())) {
            throw new AuthException("Token temporal inválido o expirado");
        }
        Long userId = jwtService.extractUserId(req.tempToken());

        User user;
        try {
            user = userDao.findById(userId.intValue());
            try {
                String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel());
                return new LoginResponse(false, null, accessToken, user.getLevel());
            } catch (Exception e) {
                log.error("[AUTH] Error generando access token para usuario {}", user.getId(), e);
                throw e;
            }
        }
        if (user == null) {
            throw new AuthException("Usuario no encontrado");
        }

        String secret = getTotpSecret(user);
        if (!TotpUtils.verifyCode(secret, req.code())) {
            throw new AuthException("Código de autenticación inválido");
        }

        String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel());
        return new LoginResponse(false, null, accessToken, user.getLevel());
    }

    // Réplica exacta de la regla en LoginServlet/TotpServlet: nivel 4 (padre) busca
    // el secreto en PadreDao, cualquier otro nivel lo busca en ProfesorDao.
    private String getTotpSecret(User user) {
        try {
            if (user.getLevel() == 4) {
                var padre = padreDao.findById(user.getId());
                return padre != null ? padre.getTotpSecret() : null;
            } else {
                var profesor = profesorDao.findById(user.getId());
            try {
                String accessToken = jwtService.generateAccessToken((long) user.getId(), user.getLevel());
                return new LoginResponse(false, null, accessToken, user.getLevel());
            } catch (Exception e) {
                log.error("[AUTH] Error generando access token para usuario {}", user.getId(), e);
                throw e;
            }
        } catch (Exception e) {
            return null; // igual que el original: si falla la búsqueda del secreto, sigue sin 2FA
        }
    }
}