package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.PushSubscriptionDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.PushSubscriptionResponse;
import ctn.informatica.sca.dto.PushSubscriptionSaveRequest;
import ctn.informatica.sca.util.PushNotificationService;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/push-subscription")
public class PushSubscriptionController {

    private final PushSubscriptionDao pushSubscriptionDao;
    private final UserDao userDao;

    public PushSubscriptionController() {
        this(new PushSubscriptionDao(), new UserDao());
    }

    @Autowired
    public PushSubscriptionController(PushSubscriptionDao pushSubscriptionDao, UserDao userDao) {
        this.pushSubscriptionDao = pushSubscriptionDao;
        this.userDao = userDao;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public PushSubscriptionResponse getPushSubscription(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String userType = resolveUserType(userId, authentication);
        try {
            boolean subscribed = !pushSubscriptionDao.findByUser(userId, userType).isEmpty();
            return new PushSubscriptionResponse(PushNotificationService.resolveVapidPublicKey(), subscribed);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar el estado de push.", ex);
        }
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.CREATED)
    public void savePushSubscription(
            @RequestBody PushSubscriptionSaveRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String userType = resolveUserType(userId, authentication);
        if (request == null || request.endpoint() == null || request.endpoint().isBlank()
                || request.p256dh() == null || request.p256dh().isBlank()
                || request.auth() == null || request.auth().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de suscripción son requeridos.");
        }
        try {
            boolean saved = pushSubscriptionDao.save(userId, userType, request.endpoint(), request.p256dh(), request.auth());
            if (!saved) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo guardar la suscripción.");
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la suscripción.", ex);
        }
    }

    @PostMapping("/unsubscribe")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribePushSubscription(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String userType = resolveUserType(userId, authentication);
        try {
            pushSubscriptionDao.deleteByUser(userId, userType);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar la suscripción.", ex);
        }
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testPushSubscription(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String userType = resolveUserType(userId, authentication);
        try {
            boolean delivered = PushNotificationService.sendToUser(userId, userType, "Prueba CTN", "Esta es una notificación de prueba.", "/perfil");
            int subscriptionCount = pushSubscriptionDao.findByUser(userId, userType).size();
            boolean hasVapidKeys = !PushNotificationService.resolveVapidPublicKey().isBlank() && !PushNotificationService.resolveVapidPrivateKey().isBlank();
            if (!delivered) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, PushNotificationService.buildDeliveryMessage(delivered, subscriptionCount, hasVapidKeys));
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar la prueba.", ex);
        }
    }

    private String resolveUserType(int userId, Authentication authentication) {
        try {
            var user = userDao.findById(userId);
            if (user == null) {
                return "profesor";
            }
            return user.getLevel() == 4 ? "padre" : "profesor";
        } catch (Exception ex) {
            return "profesor";
        }
    }
}
