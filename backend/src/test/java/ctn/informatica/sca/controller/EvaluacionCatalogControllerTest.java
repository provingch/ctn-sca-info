package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.web.server.ResponseStatusException;

class EvaluacionCatalogControllerTest {

    private CursoDao cursoDao;
    private EspecialidadDao especialidadDao;
    private IncumplimientoRevisionDao incumplimientoRevisionDao;
    private NotificacionDao notificacionDao;
    private UserDao userDao;
    private AsignacionDao asignacionDao;
    private ProfesorDao profesorDao;
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
        controller = new EvaluacionCatalogController(
                cursoDao,
                especialidadDao,
                mock(InstrumentoDao.class),
                mock(RasgoPlanillaDao.class),
                incumplimientoRevisionDao,
                asignacionDao,
                notificacionDao,
                userDao,
                profesorDao);

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
    void resolverIncumplimiento_rechazoSinFechas_deberiaResponder400() throws Exception {
        when(incumplimientoRevisionDao.findById(9)).thenReturn(Map.of("usuarioId", 22, "asignacionId", 77, "estado", "PENDIENTE"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication));

        assertEquals(400, ex.getStatusCode().value());
        verify(incumplimientoRevisionDao).findById(9);
        verifyNoInteractions(notificacionDao);
    }

    @Test
    void resolverIncumplimiento_rechazoConFechas_validas_deberiaNotificarAlProfesor() throws Exception {
        when(incumplimientoRevisionDao.findById(9)).thenReturn(Map.of("usuarioId", 22, "asignacionId", 77, "estado", "PENDIENTE"));
        when(incumplimientoRevisionDao.resolver(9, "RECHAZADO", 14, java.time.LocalDateTime.parse("2026-08-28T10:00:00"), java.time.LocalDateTime.parse("2026-08-30T10:00:00")))
                .thenReturn(true);
        when(userDao.findById(22)).thenReturn(new User(22, "profe", "Profesor Uno", 1));
        when(notificacionDao.crear(22, "profesor", "INCUMPLIMIENTO_RESUELTO", "Incumplimiento resuelto",
                "El incumplimiento #9 fue resuelto como rechazado con suspensión desde 2026-08-28T10:00 hasta 2026-08-30T10:00",
                "INCUMPLIMIENTO_REVISION", 9L)).thenReturn(true);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of(
                "estado", "RECHAZADO",
                "suspensionDesde", "2026-08-28T10:00:00",
                "suspensionHasta", "2026-08-30T10:00:00"), authentication);

        assertEquals(true, result.get("ok"));
        assertEquals(9, result.get("id"));
        assertEquals("RECHAZADO", result.get("estado"));
        verify(incumplimientoRevisionDao).resolver(9, "RECHAZADO", 14,
                java.time.LocalDateTime.parse("2026-08-28T10:00:00"),
                java.time.LocalDateTime.parse("2026-08-30T10:00:00"));
        verify(notificacionDao).crear(22, "profesor", "INCUMPLIMIENTO_RESUELTO", "Incumplimiento resuelto",
                "El incumplimiento #9 fue resuelto como rechazado con suspensión desde 2026-08-28T10:00 hasta 2026-08-30T10:00",
                "INCUMPLIMIENTO_REVISION", 9L);
    }
}
