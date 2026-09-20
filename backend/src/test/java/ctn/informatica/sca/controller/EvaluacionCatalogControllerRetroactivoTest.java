package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.ConfiguracionSistemaDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/** Resolución individual de incongruencias retroactivas y de atrasos justificados, y de los bloqueos que generan. */
class EvaluacionCatalogControllerRetroactivoTest {

    private static final int EVALUADOR = 14;
    private static final int PROFESOR = 22;
    private static final int ASIGNACION = 30;

    private IncumplimientoRevisionDao incumplimientoDao;
    private NotificacionDao notificacionDao;
    private ConfiguracionSistemaDao configuracionDao;
    private EvaluacionCatalogController controller;
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken((long) EVALUADOR, null, List.of());

    @BeforeEach
    void setUp() throws Exception {
        incumplimientoDao = mock(IncumplimientoRevisionDao.class);
        notificacionDao = mock(NotificacionDao.class);
        configuracionDao = mock(ConfiguracionSistemaDao.class);
        when(configuracionDao.getInt("umbral_faltas_incongruencia_retroactiva", 3)).thenReturn(3);
        when(configuracionDao.getInt("umbral_atrasos_incumplimiento", 3)).thenReturn(3);
        controller = new EvaluacionCatalogController(mock(CursoDao.class), mock(EspecialidadDao.class),
                mock(InstrumentoDao.class), mock(RasgoPlanillaDao.class), incumplimientoDao, mock(AsignacionDao.class),
                notificacionDao, mock(UserDao.class), mock(ProfesorDao.class), configuracionDao);
    }

    private Map<String, Object> fila(String tipo, String estado) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("id", 9L);
        fila.put("asignacionId", ASIGNACION);
        fila.put("usuarioId", PROFESOR);
        fila.put("tipo", tipo);
        fila.put("estado", estado);
        return fila;
    }

    private void filaPendiente(String tipo) throws Exception {
        when(incumplimientoDao.findById(9)).thenReturn(fila(tipo, "PENDIENTE"));
        when(incumplimientoDao.resolver(anyInt(), anyString(), anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(incumplimientoDao.resolver(anyInt(), anyString(), anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void permitirUnaIncongruenciaNoCuentaComoFaltaNiBloquea() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "PERMITIDO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).resolver(9, "PERMITIDO", EVALUADOR, null, null);
        verify(incumplimientoDao, never()).contarRechazadosPorTipo(anyInt(), anyInt(), anyString());
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void rechazarUnaIncongruenciaNoPideFechasDeSuspension() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "INCONGRUENCIA_RETROACTIVA")).thenReturn(1L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals("RECHAZADO", result.get("estado"));
        verify(incumplimientoDao).resolver(9, "RECHAZADO", EVALUADOR, null, null);
    }

    @Test
    void conMenosRechazosQueElUmbralNoBloquea() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "INCONGRUENCIA_RETROACTIVA")).thenReturn(2L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void alLlegarAlUmbralDeRechazosCreaElBloqueoYNotificaAlProfesor() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "INCONGRUENCIA_RETROACTIVA")).thenReturn(3L);
        when(incumplimientoDao.registrarBloqueo(eq(ASIGNACION), eq(PROFESOR), eq("BLOQUEO_INCONGRUENCIA_RETROACTIVA"), anyString())).thenReturn(40);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(true, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).registrarBloqueo(ASIGNACION, PROFESOR, "BLOQUEO_INCONGRUENCIA_RETROACTIVA",
                "Se alcanzaron 3 faltas por incongruencias retroactivas — Iniciar clase bloqueado hasta revisión de evaluación.");
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("INICIAR_CLASE_BLOQUEADO"), anyString(), anyString(),
                eq("INCUMPLIMIENTO_REVISION"), eq(40L));
    }

    @Test
    void noDuplicaElBloqueoSiYaHayUnoPendiente() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "INCONGRUENCIA_RETROACTIVA")).thenReturn(4L);
        when(incumplimientoDao.existePendientePorAsignacionYUsuario(ASIGNACION, PROFESOR, "BLOQUEO_INCONGRUENCIA_RETROACTIVA")).thenReturn(true);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void elUmbralSaleDeLaConfiguracion() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(configuracionDao.getInt("umbral_faltas_incongruencia_retroactiva", 3)).thenReturn(5);
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "INCONGRUENCIA_RETROACTIVA")).thenReturn(4L);

        controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void unaIncongruenciaYaResueltaNoSeResuelveDeNuevo() throws Exception {
        when(incumplimientoDao.findById(9)).thenReturn(fila("INCONGRUENCIA_RETROACTIVA", "RECHAZADO"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void reactivarElBloqueoGuardaLaNotaYNotificaAlProfesorConElTexto() throws Exception {
        filaPendiente("BLOQUEO_INCONGRUENCIA_RETROACTIVA");

        Map<String, Object> result = controller.resolverIncumplimiento(9,
                Map.of("estado", "PERMITIDO", "nota", "Se reunió con coordinación y ajustó el plan"), authentication);

        assertEquals("PERMITIDO", result.get("estado"));
        verify(incumplimientoDao).resolver(9, "PERMITIDO", EVALUADOR, null, null, "Se reunió con coordinación y ajustó el plan");
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("BLOQUEO_RETROACTIVO_LEVANTADO"), anyString(),
                contains("Se reunió con coordinación y ajustó el plan"), eq("INCUMPLIMIENTO_REVISION"), eq(9L));
    }

    @Test
    void reactivarElBloqueoSinNotaEsError() throws Exception {
        filaPendiente("BLOQUEO_INCONGRUENCIA_RETROACTIVA");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "PERMITIDO", "nota", "  "), authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(incumplimientoDao, never()).resolver(anyInt(), anyString(), anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void elBloqueoNoSePuedeRechazar() throws Exception {
        filaPendiente("BLOQUEO_INCONGRUENCIA_RETROACTIVA");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO", "nota", "x"), authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void permitirUnAtrasoNoCuentaComoFaltaNiBloquea() throws Exception {
        filaPendiente("ATRASO");

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "PERMITIDO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).resolver(9, "PERMITIDO", EVALUADOR, null, null);
        verify(incumplimientoDao, never()).contarRechazadosPorTipo(anyInt(), anyInt(), anyString());
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void rechazarUnAtrasoNoPideFechasDeSuspensionYCuentaComoFalta() throws Exception {
        filaPendiente("ATRASO");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "ATRASO")).thenReturn(2L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).resolver(9, "RECHAZADO", EVALUADOR, null, null);
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("INCUMPLIMIENTO_RESUELTO"), anyString(),
                eq("El atraso #9 fue resuelto como rechazado"), eq("INCUMPLIMIENTO_REVISION"), eq(9L));
    }

    @Test
    void alTercerAtrasoRechazadoCreaElBloqueoDeAtrasoYNotificaAlProfesor() throws Exception {
        filaPendiente("ATRASO");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "ATRASO")).thenReturn(3L);
        when(incumplimientoDao.registrarBloqueo(eq(ASIGNACION), eq(PROFESOR), eq("BLOQUEO_ATRASO_TIEMPO_REAL"), anyString())).thenReturn(41);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(true, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).registrarBloqueo(ASIGNACION, PROFESOR, "BLOQUEO_ATRASO_TIEMPO_REAL",
                "Se alcanzaron 3 faltas por atrasos justificados — Iniciar clase bloqueado hasta revisión de evaluación.");
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("INICIAR_CLASE_BLOQUEADO"), anyString(), anyString(),
                eq("INCUMPLIMIENTO_REVISION"), eq(41L));
    }

    @Test
    void elUmbralDeAtrasosSaleDeSuPropiaConfiguracion() throws Exception {
        filaPendiente("ATRASO");
        when(configuracionDao.getInt("umbral_atrasos_incumplimiento", 3)).thenReturn(5);
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "ATRASO")).thenReturn(4L);

        controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
        verify(configuracionDao, never()).getInt("umbral_faltas_incongruencia_retroactiva", 3);
    }

    @Test
    void noDuplicaElBloqueoDeAtrasoSiYaHayUnoPendiente() throws Exception {
        filaPendiente("ATRASO");
        when(incumplimientoDao.contarRechazadosPorTipo(ASIGNACION, PROFESOR, "ATRASO")).thenReturn(4L);
        when(incumplimientoDao.existePendientePorAsignacionYUsuario(ASIGNACION, PROFESOR, "BLOQUEO_ATRASO_TIEMPO_REAL")).thenReturn(true);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao, never()).registrarBloqueo(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void reactivarElBloqueoDeAtrasoGuardaLaNotaYNotificaAlProfesorConElTexto() throws Exception {
        filaPendiente("BLOQUEO_ATRASO_TIEMPO_REAL");

        Map<String, Object> result = controller.resolverIncumplimiento(9,
                Map.of("estado", "PERMITIDO", "nota", "Regularizó las clases atrasadas"), authentication);

        assertEquals("PERMITIDO", result.get("estado"));
        verify(incumplimientoDao).resolver(9, "PERMITIDO", EVALUADOR, null, null, "Regularizó las clases atrasadas");
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("BLOQUEO_RETROACTIVO_LEVANTADO"), anyString(),
                contains("Regularizó las clases atrasadas"), eq("INCUMPLIMIENTO_REVISION"), eq(9L));
    }

    @Test
    void elBloqueoDeAtrasoNoSePuedeRechazar() throws Exception {
        filaPendiente("BLOQUEO_ATRASO_TIEMPO_REAL");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO", "nota", "x"), authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void unTipoQueNoSeResuelveDesdeAcaResponde400() throws Exception {
        when(incumplimientoDao.findById(9)).thenReturn(fila("RECHAZO", "PENDIENTE"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(incumplimientoDao, never()).resolver(anyInt(), anyString(), anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
