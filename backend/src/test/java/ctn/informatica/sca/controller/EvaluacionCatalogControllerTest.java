package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.ConfiguracionSistemaDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Profesor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class EvaluacionCatalogControllerTest {

    private CursoDao cursoDao;
    private EspecialidadDao especialidadDao;
    private IncumplimientoRevisionDao incumplimientoRevisionDao;
    private NotificacionDao notificacionDao;
    private UserDao userDao;
    private AsignacionDao asignacionDao;
    private ProfesorDao profesorDao;
    private ConfiguracionSistemaDao configuracionSistemaDao;
    private EvaluacionCatalogController controller;
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(14L, null, List.of());

    @BeforeEach
    void setUp() throws Exception {
        cursoDao = mock(CursoDao.class);
        especialidadDao = mock(EspecialidadDao.class);
        incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        notificacionDao = mock(NotificacionDao.class);
        userDao = mock(UserDao.class);
        asignacionDao = mock(AsignacionDao.class);
        profesorDao = mock(ProfesorDao.class);
        configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        when(configuracionSistemaDao.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));
        controller = new EvaluacionCatalogController(
                cursoDao,
                especialidadDao,
                mock(InstrumentoDao.class),
                mock(RasgoPlanillaDao.class),
                incumplimientoRevisionDao,
                asignacionDao,
                notificacionDao,
                userDao,
                profesorDao,
                configuracionSistemaDao);

        Profesor teacher = new Profesor();
        teacher.setId(14);
        teacher.setNombre("Ana");
        teacher.setApellido("Pérez");
        teacher.setNivel(3);
        teacher.setEspecialidadId(null);
        when(profesorDao.findById(14)).thenReturn(teacher);
        when(asignacionDao.findByProfesor(14)).thenReturn(List.of(
                new Asignacion(11, 14, 1, 1),
                new Asignacion(12, 14, 2, 2)));
    }

    @Test
    void evaluatorCatalogUsesEveryRegisteredCourse() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);

        List<EvaluacionCatalogController.CursoEvaluacionDto> result = controller.listCursos(null, authentication);

        assertEquals(2, result.size());
        verify(cursoDao).findAll();
    }

    @Test
    void profesorConAsignacionesVeSoloSusEspecialidadesYCursos() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Informática", 2027, "B"),
                new Curso(3, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);
        when(especialidadDao.findAll()).thenReturn(List.of(
                new Especialidad(1, "Informática"),
                new Especialidad(2, "Construcciones Civiles")));
        List<Asignacion> asignaciones = List.of(
                new Asignacion(11, 14, 1, 1),
                new Asignacion(12, 14, 2, 2));
        asignaciones.get(0).setEspecialidadId(1);
        asignaciones.get(0).setEspecialidad("Informática");
        asignaciones.get(1).setEspecialidadId(2);
        asignaciones.get(1).setEspecialidad("Construcciones Civiles");
        when(asignacionDao.findByProfesor(14)).thenReturn(asignaciones);
        var teacherAuth = new UsernamePasswordAuthenticationToken(14L, null, List.of(new SimpleGrantedAuthority("ROLE_LEVEL_3")));

        List<EvaluacionCatalogController.EspecialidadDto> especialidades = controller.listEspecialidades(teacherAuth);
        List<EvaluacionCatalogController.CursoEvaluacionDto> cursosResult = controller.listCursos(null, teacherAuth);

        assertEquals(List.of("Informática", "Construcciones Civiles"), especialidades.stream().map(EvaluacionCatalogController.EspecialidadDto::nombre).toList());
        assertEquals(List.of(1, 2), cursosResult.stream().map(EvaluacionCatalogController.CursoEvaluacionDto::id).toList());
    }

    @Test
    void coordinacionMantieneCatalogoInstitucionalCompleto() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);
        when(especialidadDao.findAll()).thenReturn(List.of(
                new Especialidad(1, "Informática"),
                new Especialidad(2, "Construcciones Civiles")));
        var coordAuth = new UsernamePasswordAuthenticationToken(99L, null, List.of(new SimpleGrantedAuthority("ROLE_LEVEL_5")));

        assertEquals(2, controller.listEspecialidades(coordAuth).size());
        assertEquals(2, controller.listCursos(null, coordAuth).size());
    }

    @Test
    void specialtyFilterKeepsOnlyItsAvailableSections() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Informática", 2027, "B"),
                new Curso(3, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);
        when(especialidadDao.findById(5)).thenReturn(new Especialidad(5, "Informática"));

        List<EvaluacionCatalogController.CursoEvaluacionDto> result = controller.listCursos(5, authentication);

        assertEquals(List.of("A", "B"), result.stream().map(EvaluacionCatalogController.CursoEvaluacionDto::seccion).toList());
    }

    @Test
    void resolverIncumplimiento_atrasoRechazadoSinFechas_esValidoYCuentaComoFalta() throws Exception {
        when(incumplimientoRevisionDao.findById(9)).thenReturn(Map.of("usuarioId", 22, "asignacionId", 77, "tipo", "ATRASO", "estado", "PENDIENTE"));
        when(incumplimientoRevisionDao.resolver(9, "RECHAZADO", 14, null, null)).thenReturn(true);
        when(incumplimientoRevisionDao.contarRechazadosPorTipo(77, 22, "ATRASO")).thenReturn(1L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(true, result.get("ok"));
        assertEquals("RECHAZADO", result.get("estado"));
        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoRevisionDao).contarRechazadosPorTipo(77, 22, "ATRASO");
    }

    @Test
    void resolverIncumplimiento_atrasoRechazado_notificaAlProfesorSinMencionarSuspension() throws Exception {
        when(incumplimientoRevisionDao.findById(9)).thenReturn(Map.of("usuarioId", 22, "asignacionId", 77, "tipo", "ATRASO", "estado", "PENDIENTE"));
        when(incumplimientoRevisionDao.resolver(9, "RECHAZADO", 14, null, null)).thenReturn(true);
        when(userDao.findById(22)).thenReturn(new User(22, "profe", "Profesor Uno", 1));

        controller.resolverIncumplimiento(9, Map.of(
                "estado", "RECHAZADO",
                "suspensionDesde", "2026-08-28T10:00:00",
                "suspensionHasta", "2026-08-30T10:00:00"), authentication);

        // las fechas del payload ya no significan nada: no se persisten
        verify(incumplimientoRevisionDao).resolver(9, "RECHAZADO", 14, null, null);
        verify(notificacionDao).crear(22, "profesor", "INCUMPLIMIENTO_RESUELTO", "Atraso resuelto",
                "El atraso #9 fue resuelto como rechazado", "INCUMPLIMIENTO_REVISION", 9L);
    }
}
