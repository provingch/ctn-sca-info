package ctn.informatica.sca.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Date;
import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.Tarea;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Cada planilla es de una sola etapa: el sync sólo importa/conserva los courseWork cuya fecha cae en esa etapa
 * (según el cierre de Etapa 1 de la planilla; acá el 21/06, así el corte es el 22/06) y borra —por el camino de huérfanas— los que ya estaban importados de la otra.
 */
class ClassroomSyncOrchestratorEtapaFilterTest {

    private TareaDao tareaDao;
    private GoogleClassroomAdapter adapter;
    private ClassroomSyncOrchestrator orchestrator;
    private final Profesor profesor = new Profesor();

    @BeforeEach
    void setUp() throws Exception {
        CursoDao cursoDao = mock(CursoDao.class);
        tareaDao = mock(TareaDao.class);
        adapter = mock(GoogleClassroomAdapter.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        profesor.setId(1);
        Course course = new Course();
        course.setId("C-1");
        when(cursoDao.findById(7)).thenReturn(new Curso(7, "esp", 2026, "A"));
        when(adapter.isGoogleConnected(any())).thenReturn(true);
        when(adapter.resolveCourseForPlanilla(any(), any(), any())).thenReturn(Optional.of(course));
        when(instrumentoDao.findAll()).thenReturn(List.of());
        when(alumnoDao.findByCursoId(7)).thenReturn(List.of());
        orchestrator = new ClassroomSyncOrchestrator(cursoDao, alumnoDao, tareaDao, mock(RegistroDao.class),
                mock(GradeDao.class), mock(PlanillaDao.class), instrumentoDao, adapter);
    }

    private static Planilla planilla(int id, String etapa) {
        Planilla p = new Planilla();
        p.setId(id);
        p.setCursoId(7);
        p.setEtapa(etapa);
        p.setFechaCierreEtapa1(LocalDate.of(2026, 6, 21));
        return p;
    }

    private static CourseWork courseWork(String id, int y, int m, int d) {
        CourseWork cw = new CourseWork();
        cw.setId(id);
        cw.setTitle("Tarea " + id);
        cw.setDueDate(new Date().setYear(y).setMonth(m).setDay(d));
        return cw;
    }

    private static Tarea importada(int id, int planillaId, String googleId, LocalDate fecha) {
        Tarea t = new Tarea();
        t.setId(id);
        t.setPlanillaId(planillaId);
        t.setTitulo("Tarea " + googleId);
        t.setTotal(10);
        t.setFecha(fecha);
        t.setFechaLimite(fecha);
        t.setGoogleCourseworkId(googleId);
        return t;
    }

    private void classroomTiene(CourseWork... courseWorks) throws Exception {
        when(adapter.listCourseWorkForCourse(any(), anyString())).thenReturn(List.of(courseWorks));
    }

    private List<String> titulosInsertados(int veces) throws Exception {
        ArgumentCaptor<Tarea> captor = ArgumentCaptor.forClass(Tarea.class);
        verify(tareaDao, times(veces)).insertarTarea(captor.capture());
        return captor.getAllValues().stream().map(Tarea::getGoogleCourseworkId).toList();
    }

    @Test
    void cadaPlanillaImportaSoloLosCourseWorkDeSuEtapa() throws Exception {
        CourseWork marzo = courseWork("cw-marzo", 2026, 3, 10);
        CourseWork agosto = courseWork("cw-agosto", 2026, 8, 5);
        classroomTiene(marzo, agosto);
        when(tareaDao.consultarTarea(10)).thenReturn(new ArrayList<>());
        when(tareaDao.consultarTarea(20)).thenReturn(new ArrayList<>());

        var primera = orchestrator.syncPlanillaWithClassroom(profesor, planilla(10, "primera"));
        assertEquals(1, primera.importedCourseworks());
        assertEquals(List.of("cw-marzo"), titulosInsertados(1));

        org.mockito.Mockito.clearInvocations(tareaDao);
        var segunda = orchestrator.syncPlanillaWithClassroom(profesor, planilla(20, "segunda"));
        assertEquals(1, segunda.importedCourseworks());
        assertEquals(List.of("cw-agosto"), titulosInsertados(1));
    }

    @Test
    void laTareaYaImportadaDeLaOtraEtapaSeBorraEnElSiguienteSyncPorHuerfana() throws Exception {
        // Estado previo (bug): la planilla de segunda etapa tiene importado el courseWork de marzo.
        Tarea malImportada = importada(901, 20, "cw-marzo", LocalDate.of(2026, 3, 10));
        Tarea bienImportada = importada(902, 20, "cw-agosto", LocalDate.of(2026, 8, 5));
        when(tareaDao.consultarTarea(20)).thenReturn(new ArrayList<>(List.of(malImportada, bienImportada)));
        classroomTiene(courseWork("cw-marzo", 2026, 3, 10), courseWork("cw-agosto", 2026, 8, 5));

        var resultado = orchestrator.syncPlanillaWithClassroom(profesor, planilla(20, "segunda"));

        verify(tareaDao, times(1)).delete(901);
        verify(tareaDao, never()).delete(902);
        verify(tareaDao, never()).insertarTarea(any());
        assertTrue(resultado.message().contains("1 tarea(s) eliminada(s)"), resultado.message());
    }

    @Test
    void tampocoBorraLasTareasManualesDeLaPlanilla() throws Exception {
        Tarea manual = new Tarea();
        manual.setId(903);
        manual.setPlanillaId(10);
        manual.setTitulo("Manual");
        manual.setFecha(LocalDate.of(2026, 3, 1));
        Tarea malImportada = importada(904, 10, "cw-agosto", LocalDate.of(2026, 8, 5));
        when(tareaDao.consultarTarea(10)).thenReturn(new ArrayList<>(List.of(manual, malImportada)));
        classroomTiene(courseWork("cw-agosto", 2026, 8, 5));

        orchestrator.syncPlanillaWithClassroom(profesor, planilla(10, "primera"));

        verify(tareaDao).delete(904);
        verify(tareaDao, never()).delete(903);
    }

    @Test
    void elCorteEsElDia22DeJunio() throws Exception {
        classroomTiene(courseWork("cw-21", 2026, 6, 21), courseWork("cw-22", 2026, 6, 22));
        when(tareaDao.consultarTarea(10)).thenReturn(new ArrayList<>());
        when(tareaDao.consultarTarea(20)).thenReturn(new ArrayList<>());

        orchestrator.syncPlanillaWithClassroom(profesor, planilla(10, "primera"));
        assertEquals(List.of("cw-21"), titulosInsertados(1));

        org.mockito.Mockito.clearInvocations(tareaDao);
        orchestrator.syncPlanillaWithClassroom(profesor, planilla(20, "segunda"));
        assertEquals(List.of("cw-22"), titulosInsertados(1));
    }

    @Test
    void siTodoElCourseWorkEsDeLaOtraEtapaSeLimpiaLoQueQuedabaImportado() throws Exception {
        Tarea malImportada = importada(905, 10, "cw-agosto", LocalDate.of(2026, 8, 5));
        when(tareaDao.consultarTarea(10)).thenReturn(new ArrayList<>(List.of(malImportada)));
        classroomTiene(courseWork("cw-agosto", 2026, 8, 5));

        orchestrator.syncPlanillaWithClassroom(profesor, planilla(10, "primera"));

        verify(tareaDao).delete(905);
        verify(tareaDao, never()).insertarTarea(any());
    }

    @Test
    void sinCierreDeEtapa1TodoSeImportaEnLaPrimera() throws Exception {
        classroomTiene(courseWork("cw-marzo", 2026, 3, 10), courseWork("cw-agosto", 2026, 8, 5));
        when(tareaDao.consultarTarea(10)).thenReturn(new ArrayList<>());
        Planilla sinCierre = planilla(10, "primera");
        sinCierre.setFechaCierreEtapa1(null);

        orchestrator.syncPlanillaWithClassroom(profesor, sinCierre);

        assertEquals(List.of("cw-marzo", "cw-agosto"), titulosInsertados(2));
    }
}
