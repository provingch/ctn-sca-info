package ctn.informatica.sca.dao;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalaDaoSafetyTest {
    @Test void blocksUsedRoomsWithoutDeletingOrChangingSchedules() throws Exception {
        Fixture f = new Fixture(true, true);
        assertThrows(SalaDao.SalaEnUsoException.class, () -> f.dao.eliminar(4));
        verify(f.connection).rollback();
        verify(f.delete, never()).executeUpdate();
        verify(f.connection, never()).commit();
    }
    @Test void locksRoomThenChecksUsageBeforeDeleting() throws Exception {
        Fixture f = new Fixture(true, false);
        assertTrue(f.dao.eliminar(4));
        var order = inOrder(f.parent, f.usage, f.delete, f.connection);
        order.verify(f.parent).executeQuery();
        order.verify(f.usage).executeQuery();
        order.verify(f.delete).executeUpdate();
        order.verify(f.connection).commit();
        verify(f.parent).setInt(1, 4);
        verify(f.usage).setInt(1, 4);
        verify(f.delete).setInt(1, 4);
    }
    @Test void missingRoomDoesNotDeleteAndDatabaseErrorsRollback() throws Exception {
        Fixture absent = new Fixture(false, false);
        assertFalse(absent.dao.eliminar(404));
        verify(absent.delete, never()).executeUpdate();
        Fixture failed = new Fixture(true, false);
        when(failed.delete.executeUpdate()).thenThrow(new SQLException("failed"));
        assertThrows(SQLException.class, () -> failed.dao.eliminar(4));
        verify(failed.connection).rollback();
        verify(failed.connection, never()).commit();
    }
    @Test void includesUsageCountsInTheRoomCatalog() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement query = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(contains("COUNT(*) FROM horario_slot"))).thenReturn(query);
        when(query.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getInt("id")).thenReturn(4);
        when(rows.getString("nombre")).thenReturn("S4");
        when(rows.getInt("bloques_asignados")).thenReturn(8);
        SalaDao dao = new SalaDao() { @Override public Connection getCon() { return connection; } };
        assertEquals(8, dao.findAll().get(0).getBloquesAsignados());
    }
    private static class Fixture {
        final Connection connection = mock(Connection.class);
        final PreparedStatement parent = mock(PreparedStatement.class);
        final PreparedStatement usage = mock(PreparedStatement.class);
        final PreparedStatement delete = mock(PreparedStatement.class);
        final SalaDao dao = new SalaDao() { @Override public Connection getCon() { return connection; } };
        Fixture(boolean exists, boolean used) throws Exception {
            when(connection.prepareStatement("SELECT id FROM sala WHERE id = ? FOR UPDATE")).thenReturn(parent);
            when(connection.prepareStatement("SELECT id FROM horario_slot WHERE sala_id = ? LIMIT 1 FOR UPDATE")).thenReturn(usage);
            when(connection.prepareStatement("DELETE FROM sala WHERE id = ?")).thenReturn(delete);
            ResultSet room = mock(ResultSet.class), slots = mock(ResultSet.class);
            when(parent.executeQuery()).thenReturn(room);
            when(usage.executeQuery()).thenReturn(slots);
            when(room.next()).thenReturn(exists);
            when(slots.next()).thenReturn(used);
            when(delete.executeUpdate()).thenReturn(1);
        }
    }
}
