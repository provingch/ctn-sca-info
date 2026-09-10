package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.FcmTokenDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** FCM device-token registration for the native parent app. */
@RestController
@RequestMapping("/api/push/fcm")
public class PushController {

    private final FcmTokenDao fcmTokenDao;
    private final UserDao userDao;
    private final FirebaseMessagingClient firebaseMessagingClient;

    public PushController() {
        this(new FcmTokenDao(), new UserDao(), new FirebaseMessagingClient());
    }

    @Autowired
    public PushController(FcmTokenDao fcmTokenDao, UserDao userDao, FirebaseMessagingClient firebaseMessagingClient) {
        this.fcmTokenDao = fcmTokenDao;
        this.userDao = userDao;
        this.firebaseMessagingClient = firebaseMessagingClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody FcmTokenRequest request, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String token = request == null ? null : request.token();
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token requerido");
        }
        try {
            fcmTokenDao.upsert(userId, resolveUserType(userId), token.trim(), request.platform());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo registrar el token", ex);
        }
    }

    @PostMapping("/unregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestBody FcmTokenRequest request, Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        String token = request == null ? null : request.token();
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            fcmTokenDao.deleteByToken(token.trim());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar el token", ex);
        }
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void test(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            List<String> tokens = fcmTokenDao.findTokensByUserId(userId);
            if (tokens.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay dispositivos registrados para este usuario.");
            }
            if (!firebaseMessagingClient.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servidor no tiene Firebase configurado.");
            }
            List<String> stale = firebaseMessagingClient.send(
                    tokens, "Prueba CTN", "Esta es una notificación de prueba.", null);
            if (!stale.isEmpty()) {
                fcmTokenDao.deleteByTokens(stale);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar la prueba", ex);
        }
    }

    private String resolveUserType(int userId) {
        try {
            var user = userDao.findById(userId);
            return user != null && user.getLevel() == 4 ? "padre" : "usuario";
        } catch (Exception ex) {
            return "padre";
        }
    }

    public record FcmTokenRequest(String token, String platform) {
    }
}
