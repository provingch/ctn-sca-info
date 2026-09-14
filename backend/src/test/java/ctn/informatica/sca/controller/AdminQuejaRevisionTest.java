package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.QuejaDao;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminQuejaRevisionTest {
    private final QuejaDao dao = mock(QuejaDao.class);
    private final Authentication auth = new UsernamePasswordAuthenticationToken(9, null);

    private AdminController controller(Integer scope) {
        return new AdminController(null, null, null, dao) {
            @Override protected Integer getSpecialtyAdminIdForUser(int id) { return scope; }
        };
    }

    @Test void guardaConclusionConIdentidadAutenticadaYFechaDelServidor() throws Exception {
        when(dao.findEspecialidadId(42)).thenReturn(21);
        var revision = new QuejaDao.Revision("revisada", Timestamp.valueOf("2026-09-14 12:30:00"), 9, "Resuelto");
        when(dao.completarRevision(42, 21, "Resuelto", 9)).thenReturn(revision);
        assertEquals(revision, controller(21).revisarQueja(42, new AdminController.QuejaRevisionInput("  Resuelto  "), auth));
        assertEquals(revision, controller(null).revisarQueja(42, new AdminController.QuejaRevisionInput("Resuelto"), auth));
    }

    @Test void rechazaOtraEspecialidadSinModificarLaQueja() throws Exception {
        when(dao.findEspecialidadId(42)).thenReturn(99);
        assertStatus(403, controller(21), new AdminController.QuejaRevisionInput("Resuelto"));
        verify(dao, never()).completarRevision(anyLong(), anyInt(), anyString(), anyInt());
    }

    @Test void exigeConclusionValida() {
        for (String text : new String[] { null, "", "   ", "x".repeat(5001) }) {
            assertStatus(400, controller(null), new AdminController.QuejaRevisionInput(text));
        }
        verifyNoInteractions(dao);
    }

    @Test void rechazaQuejaInexistente() throws Exception {
        when(dao.findEspecialidadId(42)).thenReturn(null);
        assertStatus(404, controller(null), new AdminController.QuejaRevisionInput("Resuelto"));
    }

    @Test void noSobrescribeUnaRevisionCompletadaPorOtroAdministrador() throws Exception {
        when(dao.findEspecialidadId(42)).thenReturn(21);
        when(dao.completarRevision(42, 21, "Resuelto", 9)).thenReturn(null);
        assertStatus(409, controller(21), new AdminController.QuejaRevisionInput("Resuelto"));
    }

    private void assertStatus(int code, AdminController controller, AdminController.QuejaRevisionInput input) {
        var ex = assertThrows(ResponseStatusException.class, () -> controller.revisarQueja(42, input, auth));
        assertEquals(code, ex.getStatusCode().value());
    }
}
