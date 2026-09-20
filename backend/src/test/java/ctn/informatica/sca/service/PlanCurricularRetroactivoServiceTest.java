package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.ClaseSinPlanDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanCurricularRetroactivoServiceTest {

    private static final int PLAN_ID = 5;
    private static final int ASIGNACION_ID = 30;
    private static final int PROFESOR_ID = 14;

    /** Plan en memoria: el cursor avanza a medida que los temas se marcan CUBIERTO, como en la base. */
    private static final class PlanEnMemoria extends TemaVerificacionService {
        final List<TemaPendiente> temas = new ArrayList<>();
        final Set<Integer> cubiertos = new HashSet<>();

        @Override
        protected TemaPendiente buscarTemaPendiente(int planId) {
            return temas.stream().filter(t -> !cubiertos.contains(t.temaId())).findFirst().orElse(null);
        }
    }

    private PlanEnMemoria plan;
    private RasgoPlanillaDao rasgoPlanillaDao;
    private PlanCurricularDao planCurricularDao;
    private IncumplimientoRevisionDao incumplimientoRevisionDao;
    private NotificacionDao notificacionDao;
    private PlanCurricularRetroactivoService service;

    @BeforeEach
    void setUp() throws Exception {
        plan = new PlanEnMemoria();
        // Etapa 1: orden 1 = Marzo ... orden 4 = Junio/Julio
        plan.temas.add(new TemaVerificacionService.TemaPendiente(1, "Unidad 1 Sistemas", 1));
        plan.temas.add(new TemaVerificacionService.TemaPendiente(2, "Unidad 2 Redes", 2));
        rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        planCurricularDao = mock(PlanCurricularDao.class);
        incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        notificacionDao = mock(NotificacionDao.class);
        doAnswer(inv -> plan.cubiertos.add(inv.getArgument(0))).when(planCurricularDao).marcarCubierto(anyInt(), anyInt());
        service = new PlanCurricularRetroactivoService(plan, rasgoPlanillaDao, planCurricularDao,
                incumplimientoRevisionDao, notificacionDao, mock(UserDao.class));
    }

    private void clases(ClaseSinPlanDto... clases) throws Exception {
        when(rasgoPlanillaDao.listarClasesSinPlan(eq(ASIGNACION_ID), eq(2026), any())).thenReturn(List.of(clases));
    }

    private static ClaseSinPlanDto clase(int id, String tema, int mes, int dia) {
        return new ClaseSinPlanDto(id, PROFESOR_ID, tema, LocalDate.of(2026, mes, dia));
    }

    @Test
    void claseQueCoincideConElTemaEsperadoQuedaOkYAvanzaElCursor() throws Exception {
        clases(clase(100, "Unidad 1 Sistemas", 3, 10), clase(101, "Unidad 2 Redes", 4, 7));

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(0, incongruencias);
        verify(planCurricularDao).marcarCubierto(1, 100);
        verify(planCurricularDao).marcarCubierto(2, 101);
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(100, "OK", 1);
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(101, "OK", 2);
        verify(incumplimientoRevisionDao, never()).registrarIncongruenciaRetroactiva(anyInt(), anyInt(), any(), anyInt(), anyString());
        verify(notificacionDao, never()).crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void claseDeUnMesPosteriorAlDelTemaPendienteQuedaAtrasadaYGeneraIncongruencia() throws Exception {
        // Junio (ordenEsperado 4) y el tema pendiente es de orden 1: atrasado.
        clases(clase(100, "Repaso general", 6, 10));

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(1, incongruencias);
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(100, "ATRASADO", 1);
        verify(planCurricularDao, never()).marcarCubierto(anyInt(), anyInt());
        verify(incumplimientoRevisionDao).registrarIncongruenciaRetroactiva(eq(ASIGNACION_ID), eq(PROFESOR_ID), eq(1), eq(100),
                org.mockito.ArgumentMatchers.contains("10/06/2026"));
    }

    @Test
    void claseQueNoCoincidePeroEstaEnFechaQuedaDudosaYTambienGeneraIncongruencia() throws Exception {
        // Marzo (ordenEsperado 1) y el pendiente es de orden 1: no está atrasado, sólo no coincide.
        clases(clase(100, "Otra cosa", 3, 10));

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(1, incongruencias);
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(100, "DUDOSO", 1);
        verify(incumplimientoRevisionDao).registrarIncongruenciaRetroactiva(eq(ASIGNACION_ID), eq(PROFESOR_ID), eq(1), eq(100), anyString());
    }

    @Test
    void variasIncongruenciasGeneranUnaSolaNotificacionAgregada() throws Exception {
        clases(clase(100, "Repaso", 6, 3), clase(101, "Otro repaso", 6, 10), clase(102, "Unidad 1 Sistemas", 6, 17));

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(2, incongruencias);
        // La tercera clase sí coincide con el tema pendiente (el cursor no avanzó con las dos anteriores).
        verify(planCurricularDao).marcarCubierto(1, 102);
        verify(notificacionDao).crear(eq(PROFESOR_ID), eq("profesor"), eq("PLAN_RETROACTIVO_INCONGRUENCIAS"), anyString(),
                org.mockito.ArgumentMatchers.contains("2 clases dadas"), eq("ASIGNACION"), eq((long) ASIGNACION_ID));
    }

    @Test
    void sinTemasPendientesCortaElLoopYDejaLasClasesRestantesEnSinPlan() throws Exception {
        plan.temas.remove(1); // un solo tema en el plan
        clases(clase(100, "Unidad 1 Sistemas", 3, 10), clase(101, "Algo más", 3, 17));

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(0, incongruencias);
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(100, "OK", 1);
        verify(rasgoPlanillaDao, never()).actualizarVerificacionPlanilla(eq(101), anyString(), any());
    }

    @Test
    void noVuelveAInsertarUnaIncongruenciaYaRegistradaParaLaMismaClase() throws Exception {
        clases(clase(100, "Repaso", 6, 10));
        when(incumplimientoRevisionDao.existeIncongruenciaRetroactivaPorClase(100)).thenReturn(true);

        int incongruencias = service.reprocesarClasesPrevias(PLAN_ID, ASIGNACION_ID, "1", 2026, PROFESOR_ID);

        assertEquals(0, incongruencias);
        verify(incumplimientoRevisionDao, never()).registrarIncongruenciaRetroactiva(anyInt(), anyInt(), any(), anyInt(), anyString());
        verify(rasgoPlanillaDao).actualizarVerificacionPlanilla(100, "ATRASADO", 1);
    }
}
