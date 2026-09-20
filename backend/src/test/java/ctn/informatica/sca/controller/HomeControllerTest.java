package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import ctn.informatica.sca.dao.HoraCatedraDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
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
import ctn.informatica.sca.dto.HorarioBloqueHoyDto;
import ctn.informatica.sca.dto.SubmitRasgoAsistenciaRequest;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.TemaVerificacionService;
import ctn.informatica.sca.service.VerificacionResultado;
import java.time.LocalTime;
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
                10, 30, 1, null, null, null, null, null, "Tema", null, List.of(), Collections.emptyMap());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createRasgoPlanilla(request, authentication(7)));

        assertEquals(403, error.getStatusCode().value());
        verify(alumnoDao, never()).findByCursoId(any(Integer.class));
    }

    @Test
    void calculaHoraFinDesdeElCatalogoRealYPersisteHorarioModalidadObservacionesEInstrumento() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        UserDao userDao = mock(UserDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        CursoBaseDao cursoBaseDao = mock(CursoBaseDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        TemaVerificacionService temaVerificacionService = mock(TemaVerificacionService.class);
        HoraCatedraDao horaCatedraDao = mock(HoraCatedraDao.class);
        HomeController controller = new HomeController(
                cursoDao, cursoBaseDao, asignacionDao, mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), alumnoDao, rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), temaVerificacionService, mock(ActivityLogService.class),
                mock(ConfiguracionSistemaDao.class), incumplimientoRevisionDao, mock(NotificacionDao.class),
                mock(QuejaDao.class), null, horaCatedraDao);

        when(userDao.findById(7)).thenReturn(new User(7, "profesor", "Profesor", 1));
        when(asignacionDao.findById(30)).thenReturn(new Asignacion(30, 7, 2, 5));
        Curso curso = new Curso(10, "Informática", 2026, "A");
        when(cursoDao.findById(10)).thenReturn(curso);
        when(cursoDao.findEspecialidadId(10)).thenReturn(1);
        when(cursoBaseDao.findId(eq(1), any(Integer.class), eq("A"))).thenReturn(5);

        Alumno alumno = new Alumno();
        alumno.setId(1);
        alumno.setNombre("Ana");
        alumno.setApellido("Gómez");
        when(alumnoDao.findByCursoId(10)).thenReturn(List.of(alumno));
        when(incumplimientoRevisionDao.existeBloqueoActivo(30)).thenReturn(false);
        when(temaVerificacionService.verificar(30, "Clase de prueba"))
                .thenReturn(new VerificacionResultado("SIN_PLAN", null, false));

        // Bloques 4 y 5 (M): 08:45-09:20 y 09:40-10:15 — hay un recreo entre
        // ambos, así que el hora_fin real (09:40 + 35) coincide con lo que
        // calcularía el front, pero por una razón distinta: acá sale del
        // catálogo, no de sumar 35 minutos a ciegas.
        when(horaCatedraDao.findAll()).thenReturn(List.of(
                new HoraCatedra(4, 4, "M", LocalTime.of(8, 45), LocalTime.of(9, 20)),
                new HoraCatedra(5, 5, "M", LocalTime.of(9, 40), LocalTime.of(10, 15))));
        when(rasgoPlanillaDao.crearPlanillaRasgo(
                anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(99);

        CreateRasgoPlanillaRequest request = new CreateRasgoPlanillaRequest(
                10, 30, 1, 5, "8:45", 2, "Presencial", "Todo bien",
                "Clase de prueba", null, List.of(), Collections.emptyMap());

        controller.createRasgoPlanilla(request, authentication(7));

        verify(rasgoPlanillaDao).crearPlanillaRasgo(
                eq(10), eq(7), eq("Clase de prueba"), isNull(),
                any(), any(), any(), eq(30),
                eq(LocalTime.of(8, 45)), eq(2), eq(LocalTime.of(10, 15)),
                eq("Presencial"), eq("Todo bien"), eq(5));
    }

    @Test
    void rechazaHorasCatedraFueraDeRango() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        UserDao userDao = mock(UserDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        CursoBaseDao cursoBaseDao = mock(CursoBaseDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        TemaVerificacionService temaVerificacionService = mock(TemaVerificacionService.class);
        HomeController controller = new HomeController(
                cursoDao, cursoBaseDao, asignacionDao, mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), alumnoDao, rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), temaVerificacionService, mock(ActivityLogService.class),
                mock(ConfiguracionSistemaDao.class), incumplimientoRevisionDao, mock(NotificacionDao.class),
                mock(QuejaDao.class), null, mock(HoraCatedraDao.class));

        when(userDao.findById(7)).thenReturn(new User(7, "profesor", "Profesor", 1));
        when(asignacionDao.findById(30)).thenReturn(new Asignacion(30, 7, 2, 5));
        Curso curso = new Curso(10, "Informática", 2026, "A");
        when(cursoDao.findById(10)).thenReturn(curso);
        when(cursoDao.findEspecialidadId(10)).thenReturn(1);
        when(cursoBaseDao.findId(eq(1), any(Integer.class), eq("A"))).thenReturn(5);
        Alumno alumno = new Alumno();
        alumno.setId(1);
        alumno.setNombre("Ana");
        alumno.setApellido("Gómez");
        when(alumnoDao.findByCursoId(10)).thenReturn(List.of(alumno));
        when(incumplimientoRevisionDao.existeBloqueoActivo(30)).thenReturn(false);
        when(temaVerificacionService.verificar(30, "Clase de prueba"))
                .thenReturn(new VerificacionResultado("SIN_PLAN", null, false));

        CreateRasgoPlanillaRequest request = new CreateRasgoPlanillaRequest(
                10, 30, 1, null, "8:45", 9, null, null,
                "Clase de prueba", null, List.of(), Collections.emptyMap());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createRasgoPlanilla(request, authentication(7)));

        assertEquals(400, error.getStatusCode().value());
        verify(rasgoPlanillaDao, never()).crearPlanillaRasgo(
                anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cadaAtrasoJustificadoCreaSuPropiaFilaPendienteDeRevision() throws Exception {
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        UserDao userDao = mock(UserDao.class);

        when(incumplimientoRevisionDao.registrarAtraso(41, 17, 9, 100, "Atraso justificado 1")).thenReturn(61);
        when(incumplimientoRevisionDao.registrarAtraso(41, 17, 10, 101, "Atraso justificado 2")).thenReturn(62);
        when(userDao.findAllByLevel(2)).thenReturn(List.of());

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), mock(AlumnoDao.class), mock(RasgoPlanillaDao.class), mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                mock(ConfiguracionSistemaDao.class), incumplimientoRevisionDao, mock(NotificacionDao.class), mock(QuejaDao.class));

        // ya no hay umbral ni conteo previo: el primer atraso ya queda pendiente de revisión
        controller.registrarIncumplimientoPorAtraso(41, 17, 9, 100, "Atraso justificado 1");
        controller.registrarIncumplimientoPorAtraso(41, 17, 10, 101, "Atraso justificado 2");

        verify(incumplimientoRevisionDao).registrarAtraso(41, 17, 9, 100, "Atraso justificado 1");
        verify(incumplimientoRevisionDao).registrarAtraso(41, 17, 10, 101, "Atraso justificado 2");
        verify(incumplimientoRevisionDao, never()).existePendientePorAsignacionYUsuario(anyInt(), anyInt(), any());
        verify(userDao, times(2)).findAllByLevel(2);
    }

    @Test
    void quejaAlcanzaUmbralCincoGeneraBandejaYPush() throws Exception {
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        UserDao userDao = mock(UserDao.class);
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        QuejaDao quejaDao = mock(QuejaDao.class);
        CursoBaseDao cursoBaseDao = mock(CursoBaseDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        when(cursoBaseDao.findEspecialidadId(13)).thenReturn(21);
        when(asignacionDao.findByProfesorAndCurso(7, 13)).thenReturn(List.of(new Asignacion(1, 7, 2, 13)));
        HomeController controller = new HomeController(
                mock(CursoDao.class), cursoBaseDao, asignacionDao, mock(ProfesorDao.class),
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
        CursoBaseDao cursoBaseDao = mock(CursoBaseDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        when(cursoBaseDao.findEspecialidadId(13)).thenReturn(21);
        when(asignacionDao.findByProfesorAndCurso(7, 13)).thenReturn(List.of(new Asignacion(1, 7, 2, 13)));
        HomeController controller = new HomeController(
                mock(CursoDao.class), cursoBaseDao, asignacionDao, mock(ProfesorDao.class),
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
    void atrasoJustificadoAvisaDeInmediatoALosEvaluadoresPorBandejaYPush() throws Exception {
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        UserDao userDao = mock(UserDao.class);
        NotificacionDao notificacionDao = mock(NotificacionDao.class);

        when(incumplimientoRevisionDao.registrarAtraso(41, 17, 9, 100, "Atraso justificado")).thenReturn(61);
        when(userDao.findAllByLevel(2)).thenReturn(List.of(new User(21, "evaluador", "Eval A", 2), new User(22, "evaluador", "Eval B", 2)));
        when(notificacionDao.crear(any(Integer.class), any(String.class), eq("INCUMPLIMIENTO"), any(String.class), any(String.class), eq("INCUMPLIMIENTO_REVISION"), eq((long) 61))).thenReturn(true);

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(AsignacionDao.class), mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), mock(AlumnoDao.class), mock(RasgoPlanillaDao.class), mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                mock(ConfiguracionSistemaDao.class), incumplimientoRevisionDao, notificacionDao, mock(QuejaDao.class));

        try (var mocked = mockStatic(PushNotificationService.class)) {
            controller.registrarIncumplimientoPorAtraso(41, 17, 9, 100, "Atraso justificado");

            mocked.verify(() -> PushNotificationService.sendToUser(eq(21), any(String.class), eq("Atraso pendiente de revisión"), any(String.class), eq("/evaluacion")));
            mocked.verify(() -> PushNotificationService.sendToUser(eq(22), any(String.class), eq("Atraso pendiente de revisión"), any(String.class), eq("/evaluacion")));
        }

        // la notificación apunta a la fila de revisión del atraso, no a la asignación
        verify(notificacionDao, times(2)).crear(any(Integer.class), any(String.class), eq("INCUMPLIMIENTO"), any(String.class), any(String.class), eq("INCUMPLIMIENTO_REVISION"), eq((long) 61));
    }

    @Test
    void agrupaHorasCatedraConsecutivasDeLaMismaAsignacionEnUnBloqueYResuelveElCursoReal() throws Exception {
        HorarioSlotDao horarioSlotDao = mock(HorarioSlotDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);

        HorarioSlot bloque1a = slotDeHoy(1, 30, 5, "Redes", "Informática 3° A", 4, "08:45", "09:20");
        HorarioSlot bloque1b = slotDeHoy(2, 30, 5, "Redes", "Informática 3° A", 5, "09:40", "10:15");
        HorarioSlot bloque2 = slotDeHoy(3, 31, 5, "Física", "Informática 3° A", 7, "10:50", "11:25");
        when(horarioSlotDao.findByProfesorYDia(eq(7), any(Integer.class)))
                .thenReturn(List.of(bloque1a, bloque1b, bloque2));

        Asignacion asignacion30 = new Asignacion();
        asignacion30.setId(30);
        asignacion30.setCursoRealId(10);
        Asignacion asignacion31 = new Asignacion();
        asignacion31.setId(31);
        asignacion31.setCursoRealId(11);
        when(asignacionDao.findByProfesor(7)).thenReturn(List.of(asignacion30, asignacion31));

        when(rasgoPlanillaDao.existeClaseParaAsignacionYFecha(eq(30), any())).thenReturn(false);
        when(rasgoPlanillaDao.existeClaseParaAsignacionYFecha(eq(31), any())).thenReturn(true);

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), asignacionDao, mock(ProfesorDao.class),
                mock(PlanillaDao.class), mock(MateriaDao.class), mock(AlumnoDao.class), rasgoPlanillaDao,
                mock(InstrumentoDao.class), mock(UserDao.class), mock(PlanCurricularDao.class),
                mock(TemaVerificacionService.class), mock(ActivityLogService.class), mock(ConfiguracionSistemaDao.class),
                mock(IncumplimientoRevisionDao.class), mock(NotificacionDao.class), mock(QuejaDao.class),
                horarioSlotDao, mock(HoraCatedraDao.class));

        List<HorarioBloqueHoyDto> bloques = controller.miHorarioHoy(authentication(7));

        assertEquals(2, bloques.size());
        HorarioBloqueHoyDto primero = bloques.get(0);
        assertEquals(30, primero.asignacionId());
        assertEquals(10, primero.cursoId());
        assertEquals("08:45", primero.horaInicio());
        assertEquals("10:15", primero.horaFin());
        assertEquals(2, primero.horasCatedra());
        assertEquals(false, primero.registrada());

        HorarioBloqueHoyDto segundo = bloques.get(1);
        assertEquals(31, segundo.asignacionId());
        assertEquals(11, segundo.cursoId());
        assertEquals(1, segundo.horasCatedra());
        assertEquals(true, segundo.registrada());
    }

    @Test
    void omiteBloquesSinCursoRealCreadoTodaviaParaLaPromocionVigente() throws Exception {
        HorarioSlotDao horarioSlotDao = mock(HorarioSlotDao.class);
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        when(horarioSlotDao.findByProfesorYDia(eq(7), any(Integer.class)))
                .thenReturn(List.of(slotDeHoy(1, 30, 5, "Redes", "Informática 1° A", 4, "08:45", "09:20")));
        when(asignacionDao.findByProfesor(7)).thenReturn(List.of());

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), asignacionDao, mock(ProfesorDao.class),
                mock(PlanillaDao.class), mock(MateriaDao.class), mock(AlumnoDao.class), mock(RasgoPlanillaDao.class),
                mock(InstrumentoDao.class), mock(UserDao.class), mock(PlanCurricularDao.class),
                mock(TemaVerificacionService.class), mock(ActivityLogService.class), mock(ConfiguracionSistemaDao.class),
                mock(IncumplimientoRevisionDao.class), mock(NotificacionDao.class), mock(QuejaDao.class),
                horarioSlotDao, mock(HoraCatedraDao.class));

        assertEquals(List.of(), controller.miHorarioHoy(authentication(7)));
    }

    private HorarioSlot slotDeHoy(int id, int asignacionId, int usuarioId, String materia, String cursoDescripcion,
            int horaCatedraNumero, String horaInicio, String horaFin) {
        HorarioSlot slot = new HorarioSlot();
        slot.setId(id);
        slot.setAsignacionId(asignacionId);
        slot.setUsuarioId(usuarioId);
        slot.setMateriaNombre(materia);
        slot.setCursoDescripcion(cursoDescripcion);
        slot.setHoraCatedraNumero(horaCatedraNumero);
        slot.setHoraInicio(horaInicio);
        slot.setHoraFin(horaFin);
        return slot;
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
