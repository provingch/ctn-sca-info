package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.FcmTokenDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget push notifications to parents about activity on their
 * children's planillas. Every public method returns immediately and never
 * propagates an error to the caller — a failed notification must not break a
 * teacher's request.
 */
@Service
public class ParentPushService {

    private static final Logger log = LoggerFactory.getLogger(ParentPushService.class);

    private final FirebaseMessagingClient fcm;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "parent-push");
        t.setDaemon(true);
        return t;
    });

    public ParentPushService(FirebaseMessagingClient fcm) {
        this.fcm = fcm;
    }

    /** A teacher published a new task on {@code planillaId}. */
    public void notifyNewTask(int planillaId, String tareaTitulo) {
        submit(() -> {
            String materia = resolveMateria(planillaId);
            List<Integer> parentIds = new PadreDao().findPadreUserIdsByPlanillaId(planillaId);
            deliver(parentIds,
                    "Nueva tarea en " + materia,
                    trim(tareaTitulo, 120),
                    Map.of("type", "nueva_tarea", "planillaId", String.valueOf(planillaId)));
        });
    }

    /** Grades were saved on {@code planillaId} for the given students. */
    public void notifyGradesSaved(int planillaId, Set<Integer> alumnoIds) {
        if (alumnoIds == null || alumnoIds.isEmpty()) {
            return;
        }
        submit(() -> {
            String materia = resolveMateria(planillaId);
            List<Integer> parentIds = new PadreDao().findPadreUserIdsByAlumnoIds(alumnoIds);
            deliver(parentIds,
                    "Calificaciones actualizadas",
                    "Se cargaron notas nuevas en " + materia + ".",
                    Map.of("type", "calificaciones", "planillaId", String.valueOf(planillaId)));
        });
    }

    private void deliver(List<Integer> parentIds, String title, String body, Map<String, String> data) throws Exception {
        if (parentIds.isEmpty()) {
            return;
        }
        FcmTokenDao tokenDao = new FcmTokenDao();
        List<String> tokens = tokenDao.findTokensByUserIds(parentIds);
        if (tokens.isEmpty()) {
            return;
        }
        List<String> stale = fcm.send(tokens, title, body, data);
        if (!stale.isEmpty()) {
            tokenDao.deleteByTokens(stale);
            log.info("Podados {} tokens FCM inválidos", stale.size());
        }
    }

    private String resolveMateria(int planillaId) {
        try {
            Planilla planilla = new PlanillaDao().findById(planillaId);
            if (planilla == null) {
                return "una materia";
            }
            Materia materia = new MateriaDao().findById(planilla.getMateriaId());
            if (materia != null && materia.getNombre() != null && !materia.getNombre().isBlank()) {
                return materia.getNombre();
            }
            return planilla.getNombre() != null && !planilla.getNombre().isBlank() ? planilla.getNombre() : "una materia";
        } catch (Exception ex) {
            return "una materia";
        }
    }

    private void submit(PushTask task) {
        try {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    log.warn("No se pudo enviar la notificación a padres: {}", ex.getMessage());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("No se pudo encolar la notificación a padres: {}", ex.getMessage());
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String v = value.strip();
        return v.length() <= max ? v : v.substring(0, max - 1) + "…";
    }

    @FunctionalInterface
    private interface PushTask {
        void run() throws Exception;
    }
}
