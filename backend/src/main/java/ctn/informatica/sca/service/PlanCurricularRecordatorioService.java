package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.util.AcademicPeriod;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Avisa al profesor que todavía no subió el plan curricular de una asignación para la etapa en curso.
 * El aviso no se repite día a día: se crea otro recién cuando el anterior ya se resolvió (el
 * {@code PLAN_PENDIENTE} se cierra solo al subir el plan, ver {@code PlanCurricularController#upload}).
 */
@Service
public class PlanCurricularRecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(PlanCurricularRecordatorioService.class);

    private final AsignacionDao asignacionDao;
    private final PlanCurricularDao planCurricularDao;
    private final NotificacionDao notificacionDao;
    private final UserDao userDao;

    @Autowired
    public PlanCurricularRecordatorioService(AsignacionDao asignacionDao, PlanCurricularDao planCurricularDao,
            NotificacionDao notificacionDao, UserDao userDao) {
        this.asignacionDao = asignacionDao;
        this.planCurricularDao = planCurricularDao;
        this.notificacionDao = notificacionDao;
        this.userDao = userDao;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledRecordatorio() {
        try {
            int creados = recordarPlanesPendientes(LocalDate.now());
            if (creados > 0) {
                log.info("Recordatorio de plan curricular: {} avisos creados", creados);
            }
        } catch (Exception ex) {
            log.error("Error generando recordatorios de plan curricular: {}", ex.getMessage(), ex);
        }
    }

    /**
     * @return cantidad de recordatorios creados
     */
    public int recordarPlanesPendientes(LocalDate hoy) throws Exception {
        // Fuera del ciclo lectivo (antes del comienzo de la etapa 1 o después de noviembre, último mes de
        // los planes) no hay plan que subir: no llenar de avisos a todos los profesores en vacaciones.
        if (hoy.isBefore(AcademicPeriod.etapaStartDate(hoy.getYear(), 1)) || hoy.getMonthValue() > 11) {
            return 0;
        }
        String etapa = String.valueOf(AcademicPeriod.etapaAt(hoy));
        int anio = hoy.getYear();

        int creados = 0;
        for (Asignacion asignacion : asignacionDao.findAll()) {
            if (asignacion.getProfesorId() <= 0) {
                continue;
            }
            try {
                if (planCurricularDao.existePlan(asignacion.getId(), etapa, anio)) {
                    continue;
                }
                if (notificacionDao.existePendientePorTipoEntidad(asignacion.getId(), "ASIGNACION", NotificacionDao.TIPO_PLAN_PENDIENTE)) {
                    continue;
                }
                notificacionDao.crear(asignacion.getProfesorId(),
                        NotificacionDao.resolveUserType(userDao, asignacion.getProfesorId()),
                        NotificacionDao.TIPO_PLAN_PENDIENTE, "Plan curricular pendiente",
                        "Todavía no subiste tu plan curricular de " + asignacion.getMateriaNombre() + " · "
                                + asignacion.getCursoOrdinal() + " " + asignacion.getCursoSeccion() + " para esta etapa.",
                        "ASIGNACION", (long) asignacion.getId());
                creados++;
            } catch (Exception ex) {
                // Una asignación con problemas no debe frenar el recordatorio de las demás.
                log.warn("No se pudo crear el recordatorio de plan para la asignación {}: {}", asignacion.getId(), ex.getMessage());
            }
        }
        return creados;
    }
}
