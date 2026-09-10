package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlumnoDaoTest {

    private static AlumnoDao daoOver(Connection con) {
        return new AlumnoDao() {
            @Override
            public Connection getCon() {
                return con;
            }
        };
    }

    @Test
    void findAllActivos_filtraPorPromocionVigenteConJoinACurso() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("nombre")).thenReturn("Ana");
        when(rs.getString("apellido")).thenReturn("Activa");
        when(rs.getInt("curso_id")).thenReturn(10);

        List<Alumno> alumnos = daoOver(con).findAllActivos();

        assertEquals(1, alumnos.size());
        verify(con).prepareStatement(argThat(sql ->
                sql.contains("JOIN curso c ON c.id = a.curso_id")
                        && sql.contains("c.promocion >= ?")
                        && !sql.contains("c.promocion <")));
        verify(ps).setInt(1, AcademicPeriod.current());
    }

    @Test
    void findAllEgresados_filtraPorPromocionPasadaYTraeEspecialidadYPromocion() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(2);
        when(rs.getString("nombre")).thenReturn("Beto");
        when(rs.getString("apellido")).thenReturn("Egresado");
        when(rs.getInt("curso_id")).thenReturn(99);
        when(rs.getString("especialidad_nombre")).thenReturn("Informática");
        when(rs.getObject("promocion")).thenReturn(2024);
        when(rs.getInt("promocion")).thenReturn(2024);

        List<Alumno> alumnos = daoOver(con).findAllEgresados();

        assertEquals(1, alumnos.size());
        assertEquals("Informática", alumnos.get(0).getEspecialidadNombre());
        assertEquals(2024, alumnos.get(0).getPromocion());
        verify(con).prepareStatement(argThat(sql ->
                sql.contains("JOIN curso c ON c.id = a.curso_id")
                        && sql.contains("JOIN especialidad e ON e.id = c.especialidad_id")
                        && sql.contains("c.promocion < ?")
                        && !sql.contains("c.promocion >=")));
        verify(ps).setInt(eq(1), eq(AcademicPeriod.current()));
    }
}
