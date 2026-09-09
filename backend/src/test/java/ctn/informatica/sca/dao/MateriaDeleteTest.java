package ctn.informatica.sca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MateriaDeleteTest {
    private final Connection connection = mock(Connection.class);
    private final MateriaDao dao = new MateriaDao() {
        @Override public Connection getCon() { return connection; }
    };
    private final PreparedStatement deleteLinks = mock(PreparedStatement.class);
    private final PreparedStatement deleteSubject = mock(PreparedStatement.class);
    private ResultSet subject;
    private ResultSet sheets;
    private ResultSet assignments;

    private ResultSet query(String sql) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.prepareStatement(sql)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        return result;
    }

    @BeforeEach void setup() throws SQLException {
        subject = query("SELECT id FROM materia WHERE id = ? FOR UPDATE");
        sheets = query("SELECT 1 FROM planilla WHERE materia_id = ? LIMIT 1");
        assignments = query("SELECT 1 FROM asignacion WHERE materia_id = ? LIMIT 1");
        when(subject.next()).thenReturn(true);
        when(connection.prepareStatement("DELETE FROM materia_especialidad WHERE materia_id = ?")).thenReturn(deleteLinks);
        when(connection.prepareStatement("DELETE FROM materia WHERE id = ?")).thenReturn(deleteSubject);
        when(deleteSubject.executeUpdate()).thenReturn(1);
    }

    @Test void deletesBothRecordsInOneTransaction() throws SQLException {
        assertTrue(dao.delete(7));
        var order = inOrder(connection, deleteLinks, deleteSubject);
        order.verify(connection).setAutoCommit(false);
        order.verify(deleteLinks).executeUpdate();
        order.verify(deleteSubject).executeUpdate();
        order.verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test void rollsBackSpecialtiesIfSubjectDeletionFails() throws SQLException {
        when(deleteSubject.executeUpdate()).thenThrow(new SQLException("Foreign key constraint"));
        assertThrows(SQLException.class, () -> dao.delete(7));
        verify(deleteLinks).executeUpdate();
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test void blocksSubjectsWithSheets() throws SQLException {
        when(sheets.next()).thenReturn(true);
        assertFalse(dao.delete(7));
        verifyNoInteractions(deleteLinks, deleteSubject);
        verify(connection).rollback();
    }

    @Test void blocksSubjectsWithAssignments() throws SQLException {
        when(assignments.next()).thenReturn(true);
        assertFalse(dao.delete(7));
        verifyNoInteractions(deleteLinks, deleteSubject);
        verify(connection).rollback();
    }

    @Test void doesNotDeleteLinksForMissingSubject() throws SQLException {
        when(subject.next()).thenReturn(false);
        assertFalse(dao.delete(7));
        verifyNoInteractions(deleteLinks, deleteSubject);
        verify(connection).rollback();
    }
}
