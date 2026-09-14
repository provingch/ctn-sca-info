package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.QuejaDao;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuejaDocumentosTest {
    private final QuejaDao dao = mock(QuejaDao.class);
    private final Authentication auth = new UsernamePasswordAuthenticationToken(9, null);
    private final Timestamp date = Timestamp.valueOf("2026-09-14 10:00:00");
    private AdminController controller(Integer scope) {
        return new AdminController(null, null, null, dao) {
            @Override protected Integer getSpecialtyAdminIdForUser(int userId) { return scope; }
        };
    }
    private QuejaDao.Documento doc(boolean reviewed, boolean resolved) {
        return new QuejaDao.Documento(42, 21, "Informática", "Ana Pérez", "2° A", "Queja original.", date,
                reviewed ? date : null, "Revisión", "Pasos realizados", "Solución", "María López", resolved ? date : null);
    }
    private void status(int code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
    @Test void rechazaAusentesYOtrasEspecialidadesEnAmbasDescargas() throws Exception {
        var scoped = controller(99);
        status(404, () -> scoped.quejaSolicitudExcel(42, auth));
        status(404, () -> scoped.quejaSolucionPdf(42, auth));
        when(dao.documento(42)).thenReturn(doc(false, false));
        status(403, () -> scoped.quejaSolicitudExcel(42, auth));
        status(403, () -> scoped.quejaSolucionPdf(42, auth));
    }
    @Test void bloqueaDescargasSegunEstadoInclusoPorUrlDirecta() throws Exception {
        var admin = controller(null);
        when(dao.documento(42)).thenReturn(doc(false, false));
        status(409, () -> admin.quejaSolucionPdf(42, auth));
        when(dao.documento(42)).thenReturn(doc(true, false));
        status(409, () -> admin.quejaSolicitudExcel(42, auth));
        status(409, () -> admin.quejaSolucionPdf(42, auth));
        when(dao.documento(42)).thenReturn(doc(true, true));
        status(409, () -> admin.quejaSolicitudExcel(42, auth));
    }
    @Test void entregaArchivosConNombreTipoYCacheCorrectos() throws Exception {
        when(dao.documento(42)).thenReturn(doc(false, false));
        var excel = controller(21).quejaSolicitudExcel(42, auth);
        assertTrue(excel.getHeaders().getFirst("Content-Disposition").contains("solicitud-revision-42.xlsx"));
        assertTrue(excel.getHeaders().getFirst("Content-Type").contains("spreadsheetml"));
        assertEquals("no-store", excel.getHeaders().getFirst("Cache-Control"));
        assertEquals('P', excel.getBody()[0]); assertEquals('K', excel.getBody()[1]);
        when(dao.documento(42)).thenReturn(doc(true, true));
        var pdf = controller(null).quejaSolucionPdf(42, auth);
        assertTrue(pdf.getHeaders().getFirst("Content-Disposition").contains("reporte-solucion-42.pdf"));
        assertEquals("application/pdf", pdf.getHeaders().getFirst("Content-Type"));
        assertEquals('%', pdf.getBody()[0]);
    }
    @Test void validaResolucionYConservaResponsableRealDelRegistro() throws Exception {
        var admin = controller(21);
        var input = new AdminController.QuejaResolucionInput(" Pasos ", " Solución ", " María López ");
        when(dao.documento(42)).thenReturn(doc(true, false));
        when(dao.resolver(42, 21, "Pasos", "Solución", "María López", 9)).thenReturn(new QuejaDao.Resolucion("resuelta", "Pasos", "Solución", "María López", date));
        assertEquals("resuelta", admin.resolverQueja(42, input, auth).estado());
        verify(dao).resolver(42, 21, "Pasos", "Solución", "María López", 9);
        status(400, () -> admin.resolverQueja(42, new AdminController.QuejaResolucionInput(" ", "Solución", "Nombre"), auth));
        status(400, () -> admin.resolverQueja(42, new AdminController.QuejaResolucionInput("x".repeat(5001), "Solución", "Nombre"), auth));
        status(403, () -> controller(99).resolverQueja(42, input, auth));
        when(dao.resolver(anyLong(), anyInt(), anyString(), anyString(), anyString(), anyInt())).thenReturn(null);
        status(409, () -> admin.resolverQueja(42, input, auth));
    }
}
