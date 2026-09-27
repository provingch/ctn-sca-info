package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.FcmTokenDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PadreDao padreDao;
    private final NotificacionDao notificacionDao;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "parent-push");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public ParentPushService(FirebaseMessagingClient fcm, PadreDao padreDao, NotificacionDao notificacionDao) {
        this.fcm = fcm;
        this.padreDao = padreDao;
        this.notificacionDao = notificacionDao;
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

    /**
     * A child gained a pending "novedad" (grade saved, task published, or conduct code
     * assigned): register one unread {@code NOVEDAD_ALUMNO} row per (padre, alumno) so the
     * daily reminder ({@code ParentRecordatorioService}) picks it up. Does not push immediately;
     * deduplicated — a padre already sitting on an unread novedad for that child doesn't get a
     * second row.
     */
    public void notifyNovedadAlumnos(Set<Integer> alumnoIds) {
        if (alumnoIds == null || alumnoIds.isEmpty()) {
            return;
        }
        submit(() -> registrarNovedadAlumnos(alumnoIds));
    }

    /** Synchronous core of {@link #notifyNovedadAlumnos}, package-visible so tests can call it without the executor. */
    void registrarNovedadAlumnos(Set<Integer> alumnoIds) {
        for (Integer alumnoId : alumnoIds) {
            List<Padre> padres;
            try {
                padres = padreDao.findPadresByAlumnoId(alumnoId);
            } catch (Exception ex) {
                log.warn("No se pudieron resolver los padres del alumno {}: {}", alumnoId, ex.getMessage());
                continue;
            }
            for (Padre padre : padres) {
                try {
                    if (notificacionDao.existePendienteParaUsuario(padre.getId(), "padre",
                            NotificacionDao.TIPO_NOVEDAD_ALUMNO, NotificacionDao.ENTIDAD_ALUMNO, alumnoId)) {
                        continue;
                    }
                    notificacionDao.crear(padre.getId(), "padre", NotificacionDao.TIPO_NOVEDAD_ALUMNO,
                            "Novedades académicas", "Hay novedades para revisar.",
                            NotificacionDao.ENTIDAD_ALUMNO, (long) alumnoId);
                } catch (Exception ex) {
                    log.warn("No se pudo registrar la novedad del alumno {} para el padre {}: {}", alumnoId, padre.getId(), ex.getMessage());
                }
            }
        }
    }

    /** Package-visible: {@code ParentRecordatorioService} reúsa la resolución de tokens y la poda de inválidos. */
    void deliver(List<Integer> parentIds, String title, String body, Map<String, String> data) throws Exception {
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
