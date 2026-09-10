package ctn.informatica.sca.dao;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TextEncodingMigrationDaoTest {
    @Test void skipsAnAlreadyAppliedRepair() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement check = mock(PreparedStatement.class);
        ResultSet exists = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(check);
        when(check.executeQuery()).thenReturn(exists);
        when(exists.next()).thenReturn(true);
        TextEncodingMigrationDao dao = new TextEncodingMigrationDao() {
            @Override public Connection getCon() { return connection; }
        };
        assertEquals(0, dao.repairOnce());
        verify(connection, never()).createStatement();
        verify(connection, never()).commit();
    }

    @Test void backsUpBeforeRepairingAndCommitsTheVersionTogether() throws Exception {
        Fixture fixture = new Fixture();
        assertEquals(1, fixture.dao.repairOnce());
        var order = inOrder(fixture.backup, fixture.update, fixture.record, fixture.connection);
        order.verify(fixture.backup).executeUpdate();
        order.verify(fixture.update).executeUpdate();
        order.verify(fixture.record).executeUpdate();
        order.verify(fixture.connection).commit();
        verify(fixture.backup).setBytes(4, "Electr\u00c3\u00b3nica".getBytes(StandardCharsets.UTF_8));
        verify(fixture.update).setString(1, "Electrónica");
    }

    @Test void rollsBackIfUpdatingFailsAndDoesNotMarkApplied() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.update.executeUpdate()).thenThrow(new SQLException("update failed"));
        assertThrows(SQLException.class, fixture.dao::repairOnce);
        verify(fixture.connection).rollback();
        verify(fixture.connection, never()).commit();
        verify(fixture.record, never()).executeUpdate();
    }

    private static class Fixture {
        final Connection connection = mock(Connection.class);
        final PreparedStatement backup = mock(PreparedStatement.class);
        final PreparedStatement update = mock(PreparedStatement.class);
        final PreparedStatement record = mock(PreparedStatement.class);
        final TextEncodingMigrationDao dao = new TextEncodingMigrationDao() {
            @Override public Connection getCon() { return connection; }
        };
        Fixture() throws Exception {
            PreparedStatement check = mock(PreparedStatement.class);
            ResultSet unapplied = mock(ResultSet.class);
            when(connection.prepareStatement(startsWith("SELECT 1 FROM schema_migrations"))).thenReturn(check);
            when(check.executeQuery()).thenReturn(unapplied);
            when(connection.createStatement()).thenReturn(mock(Statement.class));
            PreparedStatement metadata = mock(PreparedStatement.class);
            ResultSet columns = mock(ResultSet.class);
            when(connection.prepareStatement(contains("information_schema"))).thenReturn(metadata);
            when(metadata.executeQuery()).thenReturn(columns);
            when(columns.next()).thenReturn(true, false); // Only especialidad.nombre exists.
            PreparedStatement select = mock(PreparedStatement.class);
            ResultSet rows = mock(ResultSet.class);
            when(connection.prepareStatement(startsWith("SELECT id,"))).thenReturn(select);
            when(select.executeQuery()).thenReturn(rows);
            when(rows.next()).thenReturn(true, true, false);
            when(rows.getString(2)).thenReturn("Electr\u00c3\u00b3nica", "Matemática");
            when(rows.getLong(1)).thenReturn(2L);
            when(connection.prepareStatement(startsWith("INSERT INTO text_encoding"))).thenReturn(backup);
            when(connection.prepareStatement(startsWith("UPDATE"))).thenReturn(update);
            when(update.executeUpdate()).thenReturn(1);
            when(connection.prepareStatement(startsWith("INSERT INTO schema_migrations"))).thenReturn(record);
        }
    }
}
