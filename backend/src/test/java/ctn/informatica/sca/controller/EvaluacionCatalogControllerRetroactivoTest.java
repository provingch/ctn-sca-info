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

/** Resolución de incongruencias retroactivas y del bloqueo que generan (el flujo de atraso en vivo no cambia). */
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
        verify(incumplimientoDao, never()).contarRechazadosRetroactivos(anyInt(), anyInt());
        verify(incumplimientoDao, never()).registrarBloqueoIncongruenciaRetroactiva(anyInt(), anyInt(), anyString());
    }

    @Test
    void rechazarUnaIncongruenciaNoPideFechasDeSuspension() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosRetroactivos(ASIGNACION, PROFESOR)).thenReturn(1L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals("RECHAZADO", result.get("estado"));
        verify(incumplimientoDao).resolver(9, "RECHAZADO", EVALUADOR, null, null);
    }

    @Test
    void conMenosRechazosQueElUmbralNoBloquea() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosRetroactivos(ASIGNACION, PROFESOR)).thenReturn(2L);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao, never()).registrarBloqueoIncongruenciaRetroactiva(anyInt(), anyInt(), anyString());
    }

    @Test
    void alLlegarAlUmbralDeRechazosCreaElBloqueoYNotificaAlProfesor() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosRetroactivos(ASIGNACION, PROFESOR)).thenReturn(3L);
        when(incumplimientoDao.registrarBloqueoIncongruenciaRetroactiva(eq(ASIGNACION), eq(PROFESOR), anyString())).thenReturn(40);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(true, result.get("bloqueoGenerado"));
        verify(incumplimientoDao).registrarBloqueoIncongruenciaRetroactiva(ASIGNACION, PROFESOR,
                "Se alcanzaron 3 faltas por incongruencias retroactivas — Iniciar clase bloqueado hasta revisión de evaluación.");
        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("INICIAR_CLASE_BLOQUEADO"), anyString(), anyString(),
                eq("INCUMPLIMIENTO_REVISION"), eq(40L));
    }

    @Test
    void noDuplicaElBloqueoSiYaHayUnoPendiente() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(incumplimientoDao.contarRechazadosRetroactivos(ASIGNACION, PROFESOR)).thenReturn(4L);
        when(incumplimientoDao.existePendientePorAsignacionYUsuario(ASIGNACION, PROFESOR, "BLOQUEO_INCONGRUENCIA_RETROACTIVA")).thenReturn(true);

        Map<String, Object> result = controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        assertEquals(false, result.get("bloqueoGenerado"));
        verify(incumplimientoDao, never()).registrarBloqueoIncongruenciaRetroactiva(anyInt(), anyInt(), anyString());
    }

    @Test
    void elUmbralSaleDeLaConfiguracion() throws Exception {
        filaPendiente("INCONGRUENCIA_RETROACTIVA");
        when(configuracionDao.getInt("umbral_faltas_incongruencia_retroactiva", 3)).thenReturn(5);
        when(incumplimientoDao.contarRechazadosRetroactivos(ASIGNACION, PROFESOR)).thenReturn(4L);

        controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication);

        verify(incumplimientoDao, never()).registrarBloqueoIncongruenciaRetroactiva(anyInt(), anyInt(), anyString());
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
    void elAtrasoEnTiempoRealSigueExigiendoSuspensionParaRechazar() throws Exception {
        when(incumplimientoDao.findById(9)).thenReturn(fila("ATRASO", "PENDIENTE"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolverIncumplimiento(9, Map.of("estado", "RECHAZADO"), authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("suspensión"));
        verify(incumplimientoDao, never()).contarRechazadosRetroactivos(anyInt(), anyInt());
        verify(notificacionDao, never()).crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }
}
