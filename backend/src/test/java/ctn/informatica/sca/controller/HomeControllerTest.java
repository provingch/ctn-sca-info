package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

import ctn.informatica.sca.util.PushNotificationService;
import java.util.Map;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.ConfiguracionSistemaDao;
import ctn.informatica.sca.dao.CursoBaseDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.QuejaDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.CreateRasgoPlanillaRequest;
import ctn.informatica.sca.dto.SubmitRasgoAsistenciaRequest;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.TemaVerificacionService;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class HomeControllerTest {

    @Test
    void noPermiteModificarAsistenciaDeClaseAjena() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        UserDao userDao = mock(UserDao.class);
        HomeController controller = controller(rasgoPlanillaDao, userDao, mock(AsignacionDao.class), mock(CursoDao.class), mock(CursoBaseDao.class), mock(AlumnoDao.class));
        when(userDao.findById(7)).thenReturn(new User(7, "profesor", "Profesor", 1));
        RasgoAsistencia asistencia = new RasgoAsistencia();
        asistencia.setId(91);
        asistencia.setPlanillaRasgoId(44);
        RasgoPlanilla claseAjena = new RasgoPlanilla();
        claseAjena.setId(44);
        claseAjena.setProfesorId(8);
        when(rasgoPlanillaDao.findAsistenciaById(91)).thenReturn(asistencia);
        when(rasgoPlanillaDao.findPlanillaById(44)).thenReturn(claseAjena);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.submitRasgoAsistencia(new SubmitRasgoAsistenciaRequest(91, "presente"), authentication(7)));

        assertEquals(403, error.getStatusCode().value());
        verify(rasgoPlanillaDao, never()).registrarRespuesta(any(Integer.class), any(String.class));
    }

    @Test
    void noPermiteCrearClaseConAsignacionAjena() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        UserDao userDao = mock(UserDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        CursoBaseDao cursoBaseDao = mock(CursoBaseDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        HomeController controller = controller(rasgoPlanillaDao, userDao, asignacionDao, cursoDao, cursoBaseDao, alumnoDao);
        when(userDao.findById(7)).thenReturn(new User(7, "profesor", "Profesor", 1));
        Asignacion ajena = new Asignacion(30, 8, 2, 5);
        when(asignacionDao.findById(30)).thenReturn(ajena);
        when(cursoDao.findById(10)).thenReturn(new Curso(10, "Informática", 2026, "A"));
        CreateRasgoPlanillaRequest request = new CreateRasgoPlanillaRequest(
                10, 30, 1, null, null, "Tema", null, List.of(), Collections.emptyMap());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createRasgoPlanilla(request, authentication(7)));

        assertEquals(403, error.getStatusCode().value());
        verify(alumnoDao, never()).findByCursoId(any(Integer.class));
    }

    @Test
    void tercerAtrasoCreaUnSoloIncumplimientoPendienteYCuartoNoDuplica() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        UserDao userDao = mock(UserDao.class);

        when(configuracionSistemaDao.getInt("umbral_atrasos_incumplimiento", 3)).thenReturn(3);
        when(rasgoPlanillaDao.contarAtrasosPorAsignacionYUsuario(41, 17)).thenReturn(3, 4);
        when(incumplimientoRevisionDao.existePendientePorAsignacionYUsuario(41, 17, "ATRASO"))
                .thenReturn(false, true);
        when(userDao.findAllByLevel(2)).thenReturn(List.of());

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), mock(AlumnoDao.class), rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                configuracionSistemaDao, incumplimientoRevisionDao, mock(NotificacionDao.class), mock(QuejaDao.class));

        controller.registrarIncumplimientoPorAtraso(41, 17, 9, "Atraso justificado 3");
        controller.registrarIncumplimientoPorAtraso(41, 17, 10, "Atraso justificado 4");

        verify(rasgoPlanillaDao, times(2)).contarAtrasosPorAsignacionYUsuario(41, 17);
        verify(incumplimientoRevisionDao, times(2)).existePendientePorAsignacionYUsuario(41, 17, "ATRASO");
        verify(incumplimientoRevisionDao, times(1)).registrar(
                eq(41), eq(17), eq(9), eq("ATRASO"), any(String.class), eq("PENDIENTE"),
                eq(null), eq(null), eq(null));
        verify(userDao, times(1)).findAllByLevel(2);
    }

    @Test
    void quejaAlcanzaUmbralCincoGeneraBandejaYPush() throws Exception {
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        UserDao userDao = mock(UserDao.class);
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        QuejaDao quejaDao = mock(QuejaDao.class);
        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class),
                mock(PlanillaDao.class), mock(MateriaDao.class), mock(AlumnoDao.class), mock(RasgoPlanillaDao.class),
                mock(InstrumentoDao.class), userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class),
                mock(ActivityLogService.class), configuracionSistemaDao, mock(IncumplimientoRevisionDao.class), notificacionDao, quejaDao);

        when(userDao.findById(9)).thenReturn(new User(9, "profesor", "Profesor", 1));
        when(configuracionSistemaDao.getInt("umbral_quejas_coordinacion", 5)).thenReturn(5);
        when(quejaDao.crear(7, 13, 21, "No responde a mensajes", 9)).thenReturn(42);
        when(quejaDao.contarPorProfesor(7)).thenReturn(5L);
        when(notificacionDao.existePendientePorTipoEntidad(any(Integer.class), eq("QUEJA"), any(String.class))).thenReturn(false);
        when(userDao.findAllByLevel(5)).thenReturn(List.of(new User(11, "coordinador", "Coord A", 5), new User(12, "coordinador", "Coord B", 5)));
        when(notificacionDao.crear(any(Integer.class), any(String.class), eq("COORDINACION"), any(String.class), any(String.class), eq("QUEJA"), eq((long) 7))).thenReturn(true);

        try (var mocked = mockStatic(PushNotificationService.class)) {
            controller.registrarQueja(Map.of(
                    "profesorId", 7,
                    "cursoId", 13,
                    "especialidadId", 21,
                    "motivo", "No responde a mensajes"
            ), authentication(9));

            mocked.verify(() -> PushNotificationService.sendToUser(eq(11), any(String.class), eq("Profesor con quejas acumuladas"), any(String.class), eq("/coordinacion")));
            mocked.verify(() -> PushNotificationService.sendToUser(eq(12), any(String.class), eq("Profesor con quejas acumuladas"), any(String.class), eq("/coordinacion")));
        }

        verify(notificacionDao, times(2)).crear(any(Integer.class), any(String.class), eq("COORDINACION"), any(String.class), any(String.class), eq("QUEJA"), eq((long) 7));
    }

    @Test
    void quejaLuegoDeSaltoDeUmbralNoDuplicaNotificacion() throws Exception {
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        UserDao userDao = mock(UserDao.class);
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        QuejaDao quejaDao = mock(QuejaDao.class);
        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class),
                mock(PlanillaDao.class), mock(MateriaDao.class), mock(AlumnoDao.class), mock(RasgoPlanillaDao.class),
                mock(InstrumentoDao.class), userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class),
                mock(ActivityLogService.class), configuracionSistemaDao, mock(IncumplimientoRevisionDao.class), notificacionDao, quejaDao);

        when(userDao.findById(9)).thenReturn(new User(9, "profesor", "Profesor", 1));
        when(configuracionSistemaDao.getInt("umbral_quejas_coordinacion", 5)).thenReturn(5);
        when(quejaDao.crear(7, 13, 21, "Reitera incumplimientos", 9)).thenReturn(43);
        when(quejaDao.contarPorProfesor(7)).thenReturn(9L);
        when(notificacionDao.existePendientePorTipoEntidad(any(Integer.class), eq("QUEJA"), any(String.class))).thenReturn(true);

        try (var mocked = mockStatic(PushNotificationService.class)) {
            controller.registrarQueja(Map.of(
                    "profesorId", 7,
                    "cursoId", 13,
                    "especialidadId", 21,
                    "motivo", "Reitera incumplimientos"
            ), authentication(9));

            mocked.verifyNoInteractions();
        }

        verify(notificacionDao, never()).crear(any(Integer.class), any(String.class), eq("COORDINACION"), any(String.class), any(String.class), eq("QUEJA"), eq((long) 7));
    }

    @Test
    void atrasoAlcanzaUmbralTresGeneraBandejaYPush() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        UserDao userDao = mock(UserDao.class);
        NotificacionDao notificacionDao = mock(NotificacionDao.class);

        when(configuracionSistemaDao.getInt("umbral_atrasos_incumplimiento", 3)).thenReturn(3);
        when(rasgoPlanillaDao.contarAtrasosPorAsignacionYUsuario(41, 17)).thenReturn(3);
        when(incumplimientoRevisionDao.existePendientePorAsignacionYUsuario(41, 17, "ATRASO")).thenReturn(false);
        when(userDao.findAllByLevel(2)).thenReturn(List.of(new User(21, "evaluador", "Eval A", 2), new User(22, "evaluador", "Eval B", 2)));
        when(notificacionDao.crear(any(Integer.class), any(String.class), eq("INCUMPLIMIENTO"), any(String.class), any(String.class), eq("INCUMPLIMIENTO_REVISION"), eq((long) 41))).thenReturn(true);

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), mock(AlumnoDao.class), rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                configuracionSistemaDao, incumplimientoRevisionDao, notificacionDao, mock(QuejaDao.class));

        try (var mocked = mockStatic(PushNotificationService.class)) {
            controller.registrarIncumplimientoPorAtraso(41, 17, 9, "Atraso justificado 3");

            mocked.verify(() -> PushNotificationService.sendToUser(eq(21), any(String.class), eq("Incumplimiento por atraso"), any(String.class), eq("/evaluacion")));
            mocked.verify(() -> PushNotificationService.sendToUser(eq(22), any(String.class), eq("Incumplimiento por atraso"), any(String.class), eq("/evaluacion")));
        }

        verify(notificacionDao, times(2)).crear(any(Integer.class), any(String.class), eq("INCUMPLIMIENTO"), any(String.class), any(String.class), eq("INCUMPLIMIENTO_REVISION"), eq((long) 41));
    }

    private HomeController controller(RasgoPlanillaDao rasgoPlanillaDao, UserDao userDao,
            AsignacionDao asignacionDao, CursoDao cursoDao, CursoBaseDao cursoBaseDao, AlumnoDao alumnoDao) {
        return new HomeController(
                cursoDao, cursoBaseDao, asignacionDao, mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), alumnoDao, rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                mock(ConfiguracionSistemaDao.class), mock(IncumplimientoRevisionDao.class), mock(NotificacionDao.class), mock(QuejaDao.class));
    }

    private Authentication authentication(int userId) {
        return new UsernamePasswordAuthenticationToken(
                (long) userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LEVEL_1")));
    }
}
