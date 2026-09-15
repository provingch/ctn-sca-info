package ctn.informatica.sca.dao;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuejaAceptacionDaoTest {
    private final Connection connection = mock(Connection.class);
    private final QuejaDao dao = new QuejaDao() {
        @Override public Connection getCon() { return connection; }
    };

    @Test void aceptaSoloUnaVez() throws Exception {
        PreparedStatement update = mock(PreparedStatement.class), read = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("UPDATE queja"))).thenReturn(update);
        when(connection.prepareStatement(startsWith("SELECT aceptada_en"))).thenReturn(read);
        when(update.executeUpdate()).thenReturn(1, 0);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        Timestamp savedAt = Timestamp.valueOf("2026-09-15 10:00:00");
        when(rows.getTimestamp(1)).thenReturn(savedAt);

        var aceptacion = dao.aceptar(42, 9);
        assertEquals(savedAt, aceptacion.aceptadaEn());
        assertEquals(9, aceptacion.aceptadaPor());
        verify(connection).prepareStatement(contains("WHERE id = ? AND aceptada_en IS NULL AND rechazada_en IS NULL"));

        assertNull(dao.aceptar(42, 10));
    }

    @Test void rechazaSoloUnaVezYGuardaElMotivo() throws Exception {
        PreparedStatement update = mock(PreparedStatement.class), read = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("UPDATE queja"))).thenReturn(update);
        when(connection.prepareStatement(startsWith("SELECT rechazada_en"))).thenReturn(read);
        when(update.executeUpdate()).thenReturn(1, 0);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        Timestamp savedAt = Timestamp.valueOf("2026-09-15 10:05:00");
        when(rows.getTimestamp(1)).thenReturn(savedAt);

        var rechazo = dao.rechazar(42, 9, "Falta contexto");
        assertEquals(savedAt, rechazo.rechazadaEn());
        assertEquals("Falta contexto", rechazo.motivoRechazo());
        verify(update).setString(2, "Falta contexto");
        verify(connection).prepareStatement(contains("WHERE id = ? AND aceptada_en IS NULL AND rechazada_en IS NULL"));

        assertNull(dao.rechazar(42, 10, "Otro motivo"));
    }

    @Test void revisionExigeQueLaQuejaEsteAceptada() throws Exception {
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("UPDATE queja"))).thenReturn(update);
        when(update.executeUpdate()).thenReturn(0);

        assertNull(dao.completarRevision(42, 21, "Conclusión", 9));
        verify(connection).prepareStatement(contains("AND revisada_en IS NULL AND aceptada_en IS NOT NULL"));
    }

    @Test void listarPriorizaRechazadaComoEstadoTerminal() throws Exception {
        PreparedStatement query = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(query);
        when(query.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        Timestamp t = Timestamp.valueOf("2026-09-15 09:00:00");
        // Una queja rechazada también tiene aceptada_en NULL: nunca fue aceptada.
        when(rows.getTimestamp("rechazada_en")).thenReturn(t);
        when(rows.getTimestamp("resuelta_en")).thenReturn(null);
        when(rows.getTimestamp("revisada_en")).thenReturn(null);
        when(rows.getTimestamp("aceptada_en")).thenReturn(null);

        var items = dao.listar();
        assertEquals("rechazada", items.get(0).get("estado"));
    }

    @Test void listarMarcaAceptadaCuandoNoHayRevisionTodavia() throws Exception {
        PreparedStatement query = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(query);
        when(query.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        Timestamp t = Timestamp.valueOf("2026-09-15 09:00:00");
        when(rows.getTimestamp("rechazada_en")).thenReturn(null);
        when(rows.getTimestamp("resuelta_en")).thenReturn(null);
        when(rows.getTimestamp("revisada_en")).thenReturn(null);
        when(rows.getTimestamp("aceptada_en")).thenReturn(t);

        var items = dao.listar();
        assertEquals("aceptada", items.get(0).get("estado"));
    }
}
