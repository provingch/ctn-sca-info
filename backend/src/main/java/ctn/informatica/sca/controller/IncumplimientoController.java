package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Incongruencias retroactivas vistas por el profesor: las propias y su justificación previa a la revisión. */
@RestController
@RequestMapping("/api/incumplimientos")
public class IncumplimientoController {

    static final int JUSTIFICACION_MAX_LEN = 2000;

    private final IncumplimientoRevisionDao incumplimientoRevisionDao;

    @Autowired
    public IncumplimientoController(IncumplimientoRevisionDao incumplimientoRevisionDao) {
        this.incumplimientoRevisionDao = incumplimientoRevisionDao;
    }

    @GetMapping("/mis-incongruencias")
    @PreAuthorize("hasRole('LEVEL_1')")
    public List<Map<String, Object>> misIncongruencias(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            return incumplimientoRevisionDao.listarIncongruenciasPendientesPorUsuario(userId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar tus incongruencias", ex);
        }
    }

    @PatchMapping("/{id}/justificar")
    @PreAuthorize("hasRole('LEVEL_1')")
    public Map<String, Object> justificar(@PathVariable int id, @RequestBody Map<String, Object> payload, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        String justificacion = payload != null && payload.get("justificacion") instanceof String s ? s.trim() : "";
        if (justificacion.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La justificación es requerida.");
        }
        if (justificacion.length() > JUSTIFICACION_MAX_LEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La justificación no puede superar los " + JUSTIFICACION_MAX_LEN + " caracteres.");
        }
        try {
            // Sólo el dueño y sólo mientras siga PENDIENTE (la query lo filtra).
            if (!incumplimientoRevisionDao.justificar(id, userId, justificacion)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró una incongruencia tuya pendiente de revisión.");
            }
            return Map.of("ok", true, "id", id);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la justificación", ex);
        }
    }
}
