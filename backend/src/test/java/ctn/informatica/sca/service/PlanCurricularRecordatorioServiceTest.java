package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Asignacion;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanCurricularRecordatorioServiceTest {

    private AsignacionDao asignacionDao;
    private PlanCurricularDao planCurricularDao;
    private NotificacionDao notificacionDao;
    private PlanCurricularRecordatorioService service;

    @BeforeEach
    void setUp() throws Exception {
        asignacionDao = mock(AsignacionDao.class);
        planCurricularDao = mock(PlanCurricularDao.class);
        notificacionDao = mock(NotificacionDao.class);
        service = new PlanCurricularRecordatorioService(asignacionDao, planCurricularDao, notificacionDao, mock(UserDao.class));
        when(asignacionDao.findAll()).thenReturn(List.of(asignacion(1, 14), asignacion(2, 15)));
    }

    private static Asignacion asignacion(int id, int profesorId) {
        Asignacion a = new Asignacion();
        a.setId(id);
        a.setProfesorId(profesorId);
        a.setMateriaNombre("Inglés");
        a.setCursoOrdinal("2º");
        a.setCursoSeccion("A");
        return a;
    }

    @Test
    void creaUnPlanPendienteParaLasAsignacionesSinPlanEnLaEtapaEnCurso() throws Exception {
        when(planCurricularDao.existePlan(1, "2", 2026)).thenReturn(true);

        int creados = service.recordarPlanesPendientes(LocalDate.of(2026, 9, 20));

        assertEquals(1, creados);
        verify(planCurricularDao).existePlan(2, "2", 2026);
        verify(notificacionDao).crear(eq(15), eq("profesor"), eq("PLAN_PENDIENTE"), eq("Plan curricular pendiente"),
                eq("Todavía no subiste tu plan curricular de Inglés · 2º A para esta etapa."), eq("ASIGNACION"), eq(2L));
        verify(notificacionDao, never()).crear(eq(14), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void noRepiteElAvisoMientrasElAnteriorSigaSinLeer() throws Exception {
        when(notificacionDao.existePendientePorTipoEntidad(1, "ASIGNACION", "PLAN_PENDIENTE")).thenReturn(true);

        int creados = service.recordarPlanesPendientes(LocalDate.of(2026, 4, 10));

        assertEquals(1, creados);
        verify(planCurricularDao).existePlan(2, "1", 2026); // abril: etapa 1
        verify(notificacionDao, never()).crear(eq(14), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void fueraDelCicloLectivoNoAvisa() throws Exception {
        assertEquals(0, service.recordarPlanesPendientes(LocalDate.of(2026, 1, 15)));
        assertEquals(0, service.recordarPlanesPendientes(LocalDate.of(2026, 12, 5)));
        verify(planCurricularDao, never()).existePlan(anyInt(), anyString(), anyInt());
    }

    @Test
    void unaAsignacionConErrorNoFrenaLasDemas() throws Exception {
        when(planCurricularDao.existePlan(1, "2", 2026)).thenThrow(new java.sql.SQLException("boom"));

        assertEquals(1, service.recordarPlanesPendientes(LocalDate.of(2026, 9, 20)));
    }
}
