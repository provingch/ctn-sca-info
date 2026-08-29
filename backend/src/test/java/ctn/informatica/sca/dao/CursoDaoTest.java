package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class CursoDaoTest {

    @Test
    void consultarCursos_usesCursoBaseIdInsteadOfNonexistentCursoIdColumn() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(con.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("id")).thenReturn(42);
        when(rs.getString("especialidad")).thenReturn("Informática");
        when(rs.getInt("promocion")).thenReturn(2026);
        when(rs.getString("seccion")).thenReturn("A");

        CursoDao dao = new CursoDao() {
            @Override
            public Connection getCon() {
                return con;
            }
        };

        ArrayList<ctn.informatica.sca.model.Curso> cursos = dao.consultarCursos(17);

        assertEquals(1, cursos.size());
        verify(con).prepareStatement(argThat(sql ->
                sql.contains("JOIN curso_base cb ON cb.id = a.curso_base_id")
                        && !sql.contains("SELECT curso_id FROM asignacion")
                        && sql.contains("cb.especialidad_id")
        ));
    }
}
