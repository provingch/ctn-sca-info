package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.UserDao;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionDao notificacionDao;
    private final UserDao userDao;

    public NotificacionController() {
        this(new NotificacionDao(), new UserDao());
    }

    public NotificacionController(NotificacionDao notificacionDao) {
        this(notificacionDao, new UserDao());
    }

    @Autowired
    public NotificacionController(NotificacionDao notificacionDao, UserDao userDao) {
        this.notificacionDao = notificacionDao;
        this.userDao = userDao;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4','LEVEL_5')")
    public List<Map<String, Object>> listar(
            @RequestParam(name = "soloNoLeidas", required = false, defaultValue = "false") boolean soloNoLeidas,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            return notificacionDao.listarPorUsuario(userId, NotificacionDao.resolveUserType(userDao, userId), soloNoLeidas);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar las notificaciones", ex);
        }
    }

    @GetMapping("/contador")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4','LEVEL_5')")
    public Map<String, Object> contador(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            long total = notificacionDao.contarNoLeidas(userId, NotificacionDao.resolveUserType(userDao, userId));
            return Map.of("count", total);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo contar las notificaciones", ex);
        }
    }

    @PostMapping("/{id}/leer")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4','LEVEL_5')")
    public Map<String, Object> marcarLeida(@PathVariable int id, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            boolean ok = notificacionDao.marcarLeida(id, userId, NotificacionDao.resolveUserType(userDao, userId));
            if (!ok) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada");
            }
            return Map.of("ok", true, "id", id);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo marcar la notificación como leída", ex);
        }
    }

    @PostMapping("/leer-todas")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4','LEVEL_5')")
    public Map<String, Object> marcarTodasLeidas(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            int actualizadas = notificacionDao.marcarTodasLeidas(userId, NotificacionDao.resolveUserType(userDao, userId));
            return Map.of("ok", true, "actualizadas", actualizadas);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron marcar las notificaciones como leídas", ex);
        }
    }
}
