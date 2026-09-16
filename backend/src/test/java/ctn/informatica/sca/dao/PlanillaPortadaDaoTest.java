package ctn.informatica.sca.dao;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlanillaPortadaDaoTest {
    private final Connection connection = mock(Connection.class);
    private final PlanillaDao dao = new PlanillaDao() {
        @Override public Connection getCon() { return connection; }
    };

    @Test void guardaLaPortadaYLaFechaDeActualizacion() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("UPDATE planilla SET portada"))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        assertTrue(dao.updatePortada(10, "data:image/png;base64,AAAA"));

        verify(ps).setString(1, "data:image/png;base64,AAAA");
        verify(ps).setTimestamp(eq(2), any(Timestamp.class));
        verify(ps).setInt(3, 10);
    }

    @Test void borrarLaPortadaGuardaNullEnAmbasColumnas() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("UPDATE planilla SET portada"))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        assertTrue(dao.updatePortada(10, null));

        verify(ps).setString(1, null);
        verify(ps).setNull(2, Types.TIMESTAMP);
    }

    @Test void devuelveFalseSiLaPlanillaNoExiste() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);

        assertFalse(dao.updatePortada(999, "data:image/png;base64,AAAA"));
    }

    @Test void findPortadaDevuelveNullSinFilas() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT portada"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertNull(dao.findPortada(10));
    }

    @Test void findPortadaDevuelveElDataUriGuardado() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT portada"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString(1)).thenReturn("data:image/webp;base64,AAAA");

        assertEquals("data:image/webp;base64,AAAA", dao.findPortada(10));
    }
}
