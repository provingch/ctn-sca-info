package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.model.Asignacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El "Docente:" del plan curricular sale de Asignacion.profesorNombre: nombre primero, apellido después. */
class AsignacionDaoNombreProfesorTest {

    @Test
    void findAll_armaElNombreCompletoEnOrdenNombreApellidoYElCorto() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("profesor_nombre")).thenReturn("Graciela Noemí");
        when(rs.getString("profesor_apellido")).thenReturn("López Molinas");
        AsignacionDao dao = new AsignacionDao() {
            @Override
            public Connection getCon() {
                return connection;
            }
        };

        List<Asignacion> asignaciones = dao.findAll();

        assertEquals(1, asignaciones.size());
        assertEquals("Graciela Noemí López Molinas", asignaciones.get(0).getProfesorNombre());
        assertEquals("Graciela López", asignaciones.get(0).getProfesorNombreCorto());
    }
}
