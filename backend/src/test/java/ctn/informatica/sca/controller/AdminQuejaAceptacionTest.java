package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.QuejaDao;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminQuejaAceptacionTest {
    private final QuejaDao dao = mock(QuejaDao.class);
    private final Authentication auth = new UsernamePasswordAuthenticationToken(9, null);
    private final AdminController controller = new AdminController(null, null, null, dao);

    @Test void aceptaYNotificaAlProfesorSinDetalleDeLaQueja() throws Exception {
        var aceptacion = new QuejaDao.Aceptacion(Timestamp.valueOf("2026-09-15 10:00:00"), 9);
        when(dao.aceptar(42, 9)).thenReturn(aceptacion);
        when(dao.findProfesorId(42)).thenReturn(7);

        var result = assertDoesNotThrow(() -> controller.aceptarQueja(42, auth));

        assertEquals(aceptacion, result);
        verify(dao).aceptar(42, 9);
        verify(dao).findProfesorId(42);
    }

    @Test void rechazaConConflictoSiYaEstabaAceptadaORechazada() throws Exception {
        when(dao.aceptar(42, 9)).thenReturn(null);
        var ex = assertThrows(ResponseStatusException.class, () -> controller.aceptarQueja(42, auth));
        assertEquals(409, ex.getStatusCode().value());
        verify(dao, never()).findProfesorId(anyLong());
    }

    @Test void rechazaQuejaConMotivoYSinNotificarANadie() throws Exception {
        var rechazo = new QuejaDao.Rechazo(Timestamp.valueOf("2026-09-15 10:00:00"), 9, "Motivo insuficiente para revisión.");
        when(dao.rechazar(42, 9, "Motivo insuficiente para revisión.")).thenReturn(rechazo);

        var result = controller.rechazarQueja(42, new AdminController.QuejaRechazoInput("  Motivo insuficiente para revisión.  "), auth);

        assertEquals(rechazo, result);
        verify(dao).rechazar(42, 9, "Motivo insuficiente para revisión.");
        verify(dao, never()).findProfesorId(anyLong());
    }

    @Test void exigeMotivoDeRechazoValido() {
        for (String motivo : new String[] { null, "", "   ", "x".repeat(5001) }) {
            var ex = assertThrows(ResponseStatusException.class,
                    () -> controller.rechazarQueja(42, new AdminController.QuejaRechazoInput(motivo), auth));
            assertEquals(400, ex.getStatusCode().value());
        }
        verifyNoInteractions(dao);
    }

    @Test void rechazoDaConflictoSiYaEstabaAceptadaORechazada() throws Exception {
        when(dao.rechazar(anyLong(), anyInt(), anyString())).thenReturn(null);
        var ex = assertThrows(ResponseStatusException.class,
                () -> controller.rechazarQueja(42, new AdminController.QuejaRechazoInput("Motivo válido"), auth));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test void unFalloAlNotificarNoRevierteLaAceptacionYaGuardada() throws Exception {
        // findProfesorId lanza: notificarAdvertenciaProfesor debe tragarse la excepción
        // (misma política que el resto de los bloques de notificación del proyecto).
        var aceptacion = new QuejaDao.Aceptacion(Timestamp.valueOf("2026-09-15 10:00:00"), 9);
        when(dao.aceptar(42, 9)).thenReturn(aceptacion);
        when(dao.findProfesorId(42)).thenThrow(new java.sql.SQLException("Notificación no disponible"));

        var result = assertDoesNotThrow(() -> controller.aceptarQueja(42, auth));
        assertEquals(aceptacion, result);
    }
}
