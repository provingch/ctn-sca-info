package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.util.AcademicPeriod;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Un push diario por padre resumiendo las novedades pendientes de sus hijos (notas cargadas,
 * tareas nuevas, códigos de conducta), registradas como filas {@code NOVEDAD_ALUMNO} por
 * {@link ParentPushService#notifyNovedadAlumnos}. Sin pendientes, no envía nada.
 */
@Service
public class ParentRecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(ParentRecordatorioService.class);

    private final NotificacionDao notificacionDao;
    private final ParentPushService parentPushService;
    private final FirebaseMessagingClient fcm;
    private final AlumnoDao alumnoDao;

    @Autowired
    public ParentRecordatorioService(NotificacionDao notificacionDao, ParentPushService parentPushService,
            FirebaseMessagingClient fcm, AlumnoDao alumnoDao) {
        this.notificacionDao = notificacionDao;
        this.parentPushService = parentPushService;
        this.fcm = fcm;
        this.alumnoDao = alumnoDao;
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "America/Asuncion")
    public void scheduledRecordatorio() {
        try {
            int enviados = enviarRecordatorios(LocalDate.now());
            if (enviados > 0) {
                log.info("Recordatorio de novedades a padres: {} avisos enviados", enviados);
            }
        } catch (Exception ex) {
            log.error("Error generando recordatorios de novedades a padres: {}", ex.getMessage(), ex);
        }
    }

    /** @return cantidad de padres a los que se les envió el resumen */
    public int enviarRecordatorios(LocalDate hoy) throws Exception {
        // Mismo criterio que PlanCurricularRecordatorioService: fuera del ciclo lectivo no hay
        // actividad académica nueva que avisar.
        if (hoy.isBefore(AcademicPeriod.etapaStartDate(hoy.getYear(), 1)) || hoy.getMonthValue() > 11) {
            return 0;
        }
        if (!fcm.isEnabled()) {
            return 0;
        }

        Map<Integer, List<Integer>> alumnosPorPadre = notificacionDao.listarAlumnosPendientesPorPadre();
        if (alumnosPorPadre.isEmpty()) {
            return 0;
        }

        int enviados = 0;
        for (Map.Entry<Integer, List<Integer>> entry : alumnosPorPadre.entrySet()) {
            int padreId = entry.getKey();
            List<Integer> alumnoIds = entry.getValue();
            try {
                String cuerpo = alumnoIds.size() == 1
                        ? "Hay novedades de " + nombreAlumno(alumnoIds.get(0)) + " para revisar"
                        : "Hay novedades de tus hijos para revisar";
                parentPushService.deliver(List.of(padreId), "Novedades académicas", cuerpo,
                        Map.of("type", "recordatorio_notas"));
                enviados++;
            } catch (Exception ex) {
                // Un padre con problemas (token vencido, etc.) no debe frenar el resumen de los demás.
                log.warn("No se pudo enviar el recordatorio de novedades al padre {}: {}", padreId, ex.getMessage());
            }
        }
        return enviados;
    }

    private String nombreAlumno(int alumnoId) {
        try {
            Alumno alumno = alumnoDao.findById(alumnoId);
            if (alumno != null && alumno.getNombre() != null && !alumno.getNombre().isBlank()) {
                return alumno.getNombre().trim();
            }
        } catch (Exception ignored) {
            // sin nombre resoluble: cae al genérico de abajo
        }
        return "tu hijo";
    }
}
