package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Planilla;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reabre una etapa ya confirmada de una planilla. Vuelve a dejar editables notas y tareas de esa etapa, por eso
 * exige un motivo, lo deja en el registro de actividad de quien la reabrió y avisa al profesor dueño.
 * Lo usan el admin global y evaluación.
 */
@Service
public class PlanillaReaperturaService {

    public static final int MOTIVO_MAX = 500;
    private static final Logger log = LoggerFactory.getLogger(PlanillaReaperturaService.class);

    /** Body de los endpoints de reapertura. */
    public record ReaperturaRequest(String motivo) {
    }

    private final PlanillaDao planillaDao;
    private final NotificacionDao notificacionDao;
    private final UserDao userDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public PlanillaReaperturaService(PlanillaDao planillaDao, NotificacionDao notificacionDao, UserDao userDao,
            ActivityLogService activityLogService) {
        this.planillaDao = planillaDao;
        this.notificacionDao = notificacionDao;
        this.userDao = userDao;
        this.activityLogService = activityLogService;
    }

    public void reabrirEtapa(int planillaId, int etapa, int actorId, String motivo) throws SQLException {
        String motivoLimpio = normalizarMotivo(motivo);
        Planilla planilla = planillaDao.findById(planillaId);
        if (planilla == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
        }
        boolean cerrada = etapa == 1 ? planilla.getEtapa1Confirmada() : planilla.getEtapa2Confirmada();
        if (!cerrada) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapa " + etapa + " no está cerrada");
        }
        boolean updated = etapa == 1 ? planillaDao.updateEtapa1Confirmada(planillaId, false) : planillaDao.updateEtapa2Confirmada(planillaId, false);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo reabrir Etapa " + etapa);
        }
        try {
            activityLogService.registrar(actorId, "Reabrió Etapa " + etapa + " de la planilla " + planillaId + " — motivo: " + motivoLimpio);
        } catch (Exception ex) {
            log.warn("No se pudo registrar el log de reapertura de Etapa {} de la planilla {}: {}", etapa, planillaId, ex.getMessage());
        }
        notificarAlProfesor(planilla, planillaId, etapa, motivoLimpio);
    }

    /**
     * El motivo termina en el registro de actividad (un archivo de texto con una línea por acción), así que se
     * colapsan los saltos de línea para que nadie pueda fabricar entradas de log falsas.
     */
    static String normalizarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debés indicar el motivo de la reapertura");
        }
        if (limpio.length() > MOTIVO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es demasiado largo (máximo " + MOTIVO_MAX + " caracteres)");
        }
        return limpio;
    }

    private void notificarAlProfesor(Planilla planilla, int planillaId, int etapa, String motivo) {
        try {
            int profesorId = planilla.getProfesorId();
            notificacionDao.crear(profesorId, NotificacionDao.resolveUserType(userDao, profesorId), "PLANILLA_REABIERTA",
                    "Planilla reabierta",
                    "Se reabrió la Etapa " + etapa + " de tu planilla: podés volver a editar sus notas y tareas. Motivo: " + motivo,
                    "PLANILLA", (long) planillaId);
        } catch (Exception ex) {
            log.warn("No se pudo notificar la reapertura de Etapa {} de la planilla {}: {}", etapa, planillaId, ex.getMessage());
        }
    }
}
