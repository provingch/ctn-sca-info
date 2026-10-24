package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.FcmTokenDao;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsuarioPushServiceTest {

    private FirebaseMessagingClient fcm;
    private FcmTokenDao tokenDao;
    private UsuarioPushService service;

    @BeforeEach
    void setUp() {
        fcm = mock(FirebaseMessagingClient.class);
        tokenDao = mock(FcmTokenDao.class);
        service = new UsuarioPushService(fcm, tokenDao);
    }

    @Test
    void soloLosCuatroTiposAcordadosLlegaranPorPush() {
        for (String tipo : List.of("INCUMPLIMIENTO_RESUELTO", "INICIAR_CLASE_BLOQUEADO", "PLAN_ACEPTADO", "PLAN_RECHAZADO")) {
            assertTrue(UsuarioPushService.corresponde("profesor", tipo), tipo);
        }
        for (String tipo : List.of("PLAN_PENDIENTE", "BLOQUEO_RETROACTIVO_LEVANTADO", "NOVEDAD_ALUMNO", "GENERAL", "")) {
            assertFalse(UsuarioPushService.corresponde("profesor", tipo), tipo);
        }
        assertFalse(UsuarioPushService.corresponde("profesor", null));
    }

    @Test
    void losPadresNoRecibenEstosAvisos() {
        assertFalse(UsuarioPushService.corresponde("padre", "PLAN_ACEPTADO"));
    }

    @Test
    void enviaTituloCuerpoYDatosALosDispositivosDelUsuario() throws Exception {
        when(tokenDao.findTokensByUserId(7)).thenReturn(List.of("t1", "t2"));
        when(fcm.send(any(), any(), any(), anyMap())).thenReturn(List.of());

        service.enviar(7, "PLAN_ACEPTADO", "Tu plan curricular fue aprobado", "Tu plan curricular fue aprobado", "PLAN_CURRICULAR", 12L);

        verify(fcm).send(eq(List.of("t1", "t2")), eq("Tu plan curricular fue aprobado"), eq("Tu plan curricular fue aprobado"),
                eq(java.util.Map.of("type", "aviso", "tipo", "PLAN_ACEPTADO", "entidadId", "12", "entidadTipo", "PLAN_CURRICULAR")));
    }

    @Test
    void sinDispositivosRegistradosNoLlamaAFirebase() throws Exception {
        when(tokenDao.findTokensByUserId(7)).thenReturn(List.of());

        service.enviar(7, "PLAN_RECHAZADO", "t", "c", "PLAN_CURRICULAR", 1L);

        verify(fcm, never()).send(any(), any(), any(), any());
    }

    @Test
    void podaLosTokensQueFirebaseMarcaComoInvalidos() throws Exception {
        when(tokenDao.findTokensByUserId(7)).thenReturn(List.of("ok", "muerto"));
        when(fcm.send(any(), any(), any(), anyMap())).thenReturn(List.of("muerto"));

        service.enviar(7, "INICIAR_CLASE_BLOQUEADO", "t", "c", null, 3L);

        verify(tokenDao).deleteByTokens(List.of("muerto"));
    }
}
