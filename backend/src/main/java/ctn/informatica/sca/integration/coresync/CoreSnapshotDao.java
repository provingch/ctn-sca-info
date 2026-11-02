package ctn.informatica.sca.integration.coresync;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CoreSnapshotDao extends conexion {

    public CoreSnapshot exportar() throws SQLException {
        try (Connection con = getCon()) {
            boolean oldAutoCommit = con.getAutoCommit();
            int oldIsolation = con.getTransactionIsolation();
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            con.setReadOnly(true);
            con.setAutoCommit(false);
            try {
                CoreSnapshot snapshot = new CoreSnapshot(
                        1,
                        LocalDateTime.now(),
                        query(con, "SELECT id, nombre FROM especialidad ORDER BY id",
                                rs -> new CoreSnapshot.Especialidad(rs.getInt("id"), rs.getString("nombre"))),
                        query(con, "SELECT id, especialidad_id, promocion, seccion FROM curso ORDER BY id",
                                rs -> new CoreSnapshot.Curso(rs.getInt("id"), rs.getInt("especialidad_id"),
                                        rs.getInt("promocion"), rs.getString("seccion"))),
                        query(con, "SELECT id, especialidad_id, nivel, seccion FROM curso_base ORDER BY id",
                                rs -> new CoreSnapshot.CursoBase(rs.getInt("id"), rs.getInt("especialidad_id"),
                                        rs.getInt("nivel"), rs.getString("seccion"))),
                        query(con, "SELECT id, nombre, categoria FROM materia ORDER BY id",
                                rs -> new CoreSnapshot.Materia(rs.getInt("id"), rs.getString("nombre"),
                                        rs.getString("categoria"))),
                        query(con, "SELECT materia_id, especialidad_id FROM materia_especialidad "
                                        + "ORDER BY materia_id, especialidad_id",
                                rs -> new CoreSnapshot.MateriaEspecialidad(rs.getInt("materia_id"),
                                        rs.getInt("especialidad_id"))),
                        query(con, "SELECT id, nombre, apellido, ci, telefono, celular, correo, nivel, especialidad_id "
                                        + "FROM usuario ORDER BY id",
                                rs -> new CoreSnapshot.Perfil(rs.getInt("id"), rs.getString("nombre"),
                                        rs.getString("apellido"), nullableInt(rs, "ci"), rs.getString("telefono"),
                                        rs.getString("celular"), rs.getString("correo"), rs.getInt("nivel"),
                                        nullableInt(rs, "especialidad_id"))),
                        query(con, "SELECT id, ci, nombre, apellido, curso_id FROM alumno ORDER BY id",
                                rs -> new CoreSnapshot.Alumno(rs.getInt("id"), rs.getString("ci"),
                                        rs.getString("nombre"), rs.getString("apellido"), rs.getInt("curso_id"))),
                        query(con, "SELECT alumno_id, usuario_id, parentesco FROM alumno_usuario "
                                        + "ORDER BY alumno_id, usuario_id",
                                rs -> new CoreSnapshot.VinculoFamiliar(rs.getInt("alumno_id"),
                                        rs.getInt("usuario_id"), rs.getString("parentesco"))),
                        query(con, "SELECT id, alumno_id, curso_anterior_id, curso_nuevo_id, fecha, usuario_id "
                                        + "FROM alumno_curso_historial ORDER BY id",
                                rs -> new CoreSnapshot.HistorialCurso(rs.getInt("id"), rs.getInt("alumno_id"),
                                        rs.getInt("curso_anterior_id"), rs.getInt("curso_nuevo_id"),
                                        localDateTime(rs, "fecha"), nullableInt(rs, "usuario_id"))),
                        query(con, "SELECT id, usuario_id, materia_id, curso_base_id FROM asignacion ORDER BY id",
                                rs -> new CoreSnapshot.Asignacion(rs.getInt("id"), rs.getInt("usuario_id"),
                                        rs.getInt("materia_id"), rs.getInt("curso_base_id"))),
                        query(con, "SELECT id, codigo, descripcion, activo, created_at FROM codigo_conducta ORDER BY id",
                                rs -> new CoreSnapshot.CodigoConducta(rs.getInt("id"), rs.getString("codigo"),
                                        rs.getString("descripcion"), rs.getBoolean("activo"),
                                        localDateTime(rs, "created_at"))));
                con.commit();
                return snapshot;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setReadOnly(false);
                con.setTransactionIsolation(oldIsolation);
                con.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private static <T> List<T> query(Connection con, String sql, RowMapper<T> mapper) throws SQLException {
        List<T> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapper.map(rs));
        }
        return result;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
