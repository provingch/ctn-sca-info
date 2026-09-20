package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.ClaseSinPlanDto;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Compara contra el plan recién aprobado las clases que se dieron cuando la asignación todavía no tenía
 * plan (quedaron en {@code SIN_PLAN}). Repite la lógica secuencial de {@link TemaVerificacionService#verificar}
 * pero con el mes real de cada clase en lugar de "hoy". Las incongruencias quedan como
 * {@code INCONGRUENCIA_RETROACTIVA} en {@code incumplimiento_revision}; no bloquean por sí solas.
 *
 * <p>Vive en el mismo paquete que {@link TemaVerificacionService} para reutilizar sus helpers protegidos.
 */
@Service
public class PlanCurricularRetroactivoService {

    private static final Logger log = LoggerFactory.getLogger(PlanCurricularRetroactivoService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemaVerificacionService temaVerificacionService;
    private final RasgoPlanillaDao rasgoPlanillaDao;
    private final PlanCurricularDao planCurricularDao;
    private final IncumplimientoRevisionDao incumplimientoRevisionDao;
    private final NotificacionDao notificacionDao;
    private final UserDao userDao;

    @Autowired
    public PlanCurricularRetroactivoService(TemaVerificacionService temaVerificacionService,
            RasgoPlanillaDao rasgoPlanillaDao, PlanCurricularDao planCurricularDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao, NotificacionDao notificacionDao, UserDao userDao) {
        this.temaVerificacionService = temaVerificacionService;
        this.rasgoPlanillaDao = rasgoPlanillaDao;
        this.planCurricularDao = planCurricularDao;
        this.incumplimientoRevisionDao = incumplimientoRevisionDao;
        this.notificacionDao = notificacionDao;
        this.userDao = userDao;
    }

    /**
     * @return cantidad de incongruencias retroactivas creadas
     */
    public int reprocesarClasesPrevias(int planId, int asignacionId, String etapa, int anioLectivo, int profesorId)
            throws SQLException {
        int etapaNumero = Integer.parseInt(etapa.trim());
        List<Integer> meses = PlanCurricularParser.mesesDeEtapa(etapa);
        List<ClaseSinPlanDto> clases = rasgoPlanillaDao.listarClasesSinPlan(asignacionId, anioLectivo, meses);

        int incongruencias = 0;
        for (ClaseSinPlanDto clase : clases) {
            int ordenEsperado = temaVerificacionService.ordenEsperadoActual(clase.fechaClase().getMonthValue(), etapaNumero);
            // Todos los temas del plan recién aprobado arrancan PENDIENTE: el cursor avanza a medida que
            // este loop marca CUBIERTO, igual que en el flujo en vivo.
            TemaVerificacionService.TemaPendiente pendiente = temaVerificacionService.buscarTemaPendiente(planId);
            if (pendiente == null) {
                // Plan ya cubierto por completo: no queda nada contra qué comparar. Las clases restantes
                // (caso raro: más clases sin plan que temas) quedan en SIN_PLAN sin más acción.
                break;
            }

            boolean atrasado = pendiente.ordenMes() != null && pendiente.ordenMes() < ordenEsperado;
            boolean coincide = TemaVerificacionService.coincidenTemas(clase.tema(), pendiente.temasContenidos());
            String estado = coincide ? "OK" : (atrasado ? "ATRASADO" : "DUDOSO");

            if (!coincide && !incumplimientoRevisionDao.existeIncongruenciaRetroactivaPorClase(clase.id())) {
                incumplimientoRevisionDao.registrarIncongruenciaRetroactiva(asignacionId, clase.usuarioId(),
                        pendiente.temaId(), clase.id(), descripcion(clase, pendiente));
                incongruencias++;
            }
            if (coincide) {
                planCurricularDao.marcarCubierto(pendiente.temaId(), clase.id());
            }
            rasgoPlanillaDao.actualizarVerificacionPlanilla(clase.id(), estado, pendiente.temaId());
        }

        if (incongruencias > 0) {
            notificarProfesor(profesorId, asignacionId, incongruencias);
        }
        return incongruencias;
    }

    private static String descripcion(ClaseSinPlanDto clase, TemaVerificacionService.TemaPendiente esperado) {
        return "Clase del " + clase.fechaClase().format(FECHA) + " dada antes de subir el plan: tema ingresado \""
                + clase.tema() + "\", tema esperado según el plan \"" + esperado.temasContenidos() + "\".";
    }

    private void notificarProfesor(int profesorId, int asignacionId, int incongruencias) {
        try {
            String cuerpo = "Se detectaron " + incongruencias + (incongruencias == 1 ? " clase dada" : " clases dadas")
                    + " antes de subir tu plan que no coinciden con lo planificado — revisalas y agregá una justificación si corresponde.";
            notificacionDao.crear(profesorId, NotificacionDao.resolveUserType(userDao, profesorId),
                    "PLAN_RETROACTIVO_INCONGRUENCIAS", "Clases previas al plan con incongruencias", cuerpo,
                    "ASIGNACION", (long) asignacionId);
        } catch (Exception ex) {
            log.warn("No se pudo notificar las incongruencias retroactivas de la asignación {}: {}", asignacionId, ex.getMessage());
        }
    }
}
