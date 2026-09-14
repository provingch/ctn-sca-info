package ctn.informatica.sca.dao;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuejaRevisionDaoTest {
    private final Connection connection = mock(Connection.class);
    private final QuejaDao dao = new QuejaDao() {
        @Override public Connection getCon() { return connection; }
    };

    @Test void guardaSoloSiSiguePendienteYEnLaMismaEspecialidad() throws Exception {
        PreparedStatement update = mock(PreparedStatement.class), read = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("UPDATE queja"))).thenReturn(update);
        when(connection.prepareStatement(startsWith("SELECT revisada_en"))).thenReturn(read);
        when(update.executeUpdate()).thenReturn(1, 0);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        Timestamp savedAt = Timestamp.valueOf("2026-09-14 12:30:00");
        when(rows.getTimestamp(1)).thenReturn(savedAt);
        var revision = dao.completarRevision(42, 21, "Resuelto", 9);
        assertEquals(savedAt, revision.revisadaEn());
        assertEquals("revisada", revision.estado());
        verify(connection).prepareStatement(contains("WHERE id = ? AND especialidad_id = ? AND revisada_en IS NULL"));
        verify(update).setInt(4, 21);
        verify(update).setInt(2, 9);
        assertNull(dao.completarRevision(42, 21, "Otra conclusión", 10));
        verify(read, times(1)).executeQuery();
    }

    @Test void historialDistinguePendientesYRevisadasConSusDatos() throws Exception {
        PreparedStatement query = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(query);
        when(query.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, true, false);
        Timestamp savedAt = Timestamp.valueOf("2026-09-14 12:30:00");
        when(rows.getTimestamp("revisada_en")).thenReturn(null, null, savedAt, savedAt);
        when(rows.getString("conclusion")).thenReturn(null, "Resuelto");
        when(rows.getObject("revisada_por")).thenReturn(null, 9);
        var items = dao.listar();
        assertEquals("pendiente", items.get(0).get("estado"));
        assertNull(items.get(0).get("revisadaEn"));
        assertEquals("revisada", items.get(1).get("estado"));
        assertEquals(savedAt, items.get(1).get("revisadaEn"));
        assertEquals("Resuelto", items.get(1).get("conclusion"));
        assertEquals(9, items.get(1).get("revisadaPor"));
    }
}
