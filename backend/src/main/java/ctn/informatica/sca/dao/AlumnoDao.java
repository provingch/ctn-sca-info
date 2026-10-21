package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.util.AcademicPeriod;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AlumnoDao extends conexion {

    /**
     * Alumnos cuyo curso todavía no egresó (promoción &gt;= período actual),
     * mismo criterio que {@link CursoDao#findAll()}. Un alumno de un curso ya
     * egresado no aparece acá — se lo consulta con {@link #findAllEgresados()}.
     */
    public List<Alumno> findAllActivos() throws SQLException {
        String sql = "SELECT a.id, a.ci, a.nombre, a.apellido, a.curso_id "
                + "FROM alumno a JOIN curso c ON c.id = a.curso_id "
                + "WHERE c.promocion >= ? "
                + "ORDER BY a.apellido, a.nombre";
        List<Alumno> alumnos = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, AcademicPeriod.current());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setCi(rs.getString("ci"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    /**
     * Alumnos cuyo curso ya egresó (promoción &lt; período actual). Trae también
     * el nombre de especialidad y el año de egreso porque esas filas de
     * {@code curso} quedan fuera de {@link CursoDao#findAll()} y el frontend no
     * podría resolverlas. Solo lectura: registro histórico.
     */
    public List<Alumno> findAllEgresados() throws SQLException {
        String sql = "SELECT a.id, a.ci, a.nombre, a.apellido, a.curso_id, "
                + "e.nombre AS especialidad_nombre, c.promocion "
                + "FROM alumno a JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE c.promocion < ? "
                + "ORDER BY c.promocion DESC, a.apellido, a.nombre";
        List<Alumno> alumnos = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, AcademicPeriod.current());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setCi(rs.getString("ci"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    alumno.setEspecialidadNombre(rs.getString("especialidad_nombre"));
                    alumno.setPromocion(rs.getObject("promocion") == null ? null : rs.getInt("promocion"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    public Alumno findById(int id) throws SQLException {
        String sql = "SELECT id, ci, nombre, apellido, curso_id FROM alumno WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setCi(rs.getString("ci"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    return alumno;
                }
            }
        }
        return null;
    }

    public List<Alumno> findByCursoId(int cursoId) throws SQLException {
        String sql = "SELECT id, nombre, apellido, curso_id, google_email FROM alumno WHERE curso_id = ? ORDER BY apellido, nombre";
        List<Alumno> alumnos = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    alumno.setGoogleEmail(rs.getString("google_email"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    public List<Alumno> findByEspecialidadId(int especialidadId) throws SQLException {
        String sql = "SELECT a.id, a.nombre, a.apellido, a.curso_id, a.google_user_id, a.google_email "
                + "FROM alumno a INNER JOIN curso c ON c.id = a.curso_id "
                + "WHERE c.especialidad_id = ? ORDER BY a.apellido, a.nombre";
        List<Alumno> alumnos = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    alumno.setGoogleUserId(rs.getString("google_user_id"));
                    alumno.setGoogleEmail(rs.getString("google_email"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    public int countByCursoId(int cursoId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM alumno WHERE curso_id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public int create(String nombre, String apellido, int cursoId, String ci) throws SQLException {
        String sql = "INSERT INTO alumno (ci, nombre, apellido, curso_id) VALUES (?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (ci == null || ci.isBlank()) {
                ps.setNull(1, java.sql.Types.VARCHAR);
            } else {
                ps.setString(1, ci.trim());
            }
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setInt(4, cursoId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                return 0;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 1;
        }
    }

    /**
     * Actualiza los datos del alumno. Si cambia de curso (traslado de especialidad o de sección) deja
     * una fila en {@code alumno_curso_historial}, en la misma transacción, para que las planillas del
     * curso viejo y la vista de padres sepan a dónde fue y desde cuándo.
     */
    public boolean update(int alumnoId, String nombre, String apellido, int cursoId, String ci, Integer usuarioId) throws SQLException {
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try {
                int cursoAnterior;
                try (PreparedStatement ps = con.prepareStatement("SELECT curso_id FROM alumno WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, alumnoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return false; }
                        cursoAnterior = rs.getInt("curso_id");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE alumno SET ci = ?, nombre = ?, apellido = ?, curso_id = ? WHERE id = ?")) {
                    if (ci == null || ci.isBlank()) {
                        ps.setNull(1, java.sql.Types.VARCHAR);
                    } else {
                        ps.setString(1, ci.trim());
                    }
                    ps.setString(2, nombre);
                    ps.setString(3, apellido);
                    ps.setInt(4, cursoId);
                    ps.setInt(5, alumnoId);
                    ps.executeUpdate();
                }
                if (cursoAnterior != cursoId) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO alumno_curso_historial (alumno_id, curso_anterior_id, curso_nuevo_id, usuario_id) VALUES (?, ?, ?, ?)")) {
                        ps.setInt(1, alumnoId);
                        ps.setInt(2, cursoAnterior);
                        ps.setInt(3, cursoId);
                        if (usuarioId == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, usuarioId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean update(int alumnoId, String nombre, String apellido, int cursoId, String ci) throws SQLException {
        return update(alumnoId, nombre, apellido, cursoId, ci, null);
    }

    public boolean updateGoogleIdentity(int alumnoId, String googleUserId, String googleEmail) throws SQLException {
        try (Connection con = getCon()) {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet columns = meta.getColumns(null, null, "alumno", "google_user_id")) {
                if (!columns.next()) {
                    return false;
                }
            }
            try (ResultSet columns = meta.getColumns(null, null, "alumno", "google_email")) {
                if (!columns.next()) {
                    return false;
                }
            }

            String sql = "UPDATE alumno SET google_user_id = ?, google_email = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, googleUserId);
                ps.setString(2, googleEmail);
                ps.setInt(3, alumnoId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    /**
     * Lo que tiene cargado un alumno, para las confirmaciones del panel: qué se pierde al borrarlo y
     * cuántas planillas de su curso actual tienen notas suyas (las que quedan atrás si se lo traslada).
     */
    public record ResumenAlumno(int notas, int planillasConNotas, int planillasConNotasCursoActual, int asistencias,
            int anotaciones, boolean fichaEnfermeria, int consultasEnfermeria) {}

    public ResumenAlumno resumen(int alumnoId) throws SQLException {
        try (Connection con = getCon()) {
            return new ResumenAlumno(
                    contar(con, "SELECT COUNT(*) FROM puntaje p JOIN registro r ON r.id = p.registro_id WHERE r.alumno_id = ?", alumnoId),
                    contar(con, "SELECT COUNT(DISTINCT r.planilla_id) FROM registro r JOIN puntaje p ON p.registro_id = r.id WHERE r.alumno_id = ?", alumnoId),
                    contar(con, "SELECT COUNT(DISTINCT r.planilla_id) FROM registro r JOIN puntaje p ON p.registro_id = r.id "
                            + "JOIN planilla pl ON pl.id = r.planilla_id JOIN alumno a ON a.id = r.alumno_id "
                            + "WHERE r.alumno_id = ? AND pl.curso_id = a.curso_id", alumnoId),
                    contar(con, "SELECT COUNT(*) FROM rasgo_asistencia WHERE alumno_id = ?", alumnoId),
                    contar(con, "SELECT COUNT(*) FROM anotacion WHERE alumno_id = ?", alumnoId),
                    contar(con, "SELECT COUNT(*) FROM enf_ficha WHERE alumno_id = ?", alumnoId) > 0,
                    contar(con, "SELECT COUNT(*) FROM enf_consulta WHERE alumno_id = ?", alumnoId));
        }
    }

    private static int contar(Connection con, String sql, int alumnoId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Tablas con FK a alumno en ON DELETE RESTRICT, en el orden en que se vacían. Sus hijas (puntaje,
     * anotacion_foto, enf_detalle_consulta) se van en cascada. rasgo_asistencia, alumno_usuario y
     * alumno_curso_historial ya tienen ON DELETE CASCADE.
     */
    private static final List<String> TABLAS_DEL_ALUMNO = List.of(
            "registro", "anotacion", "enf_consulta", "enf_detalle_alergia", "enf_tutor", "enf_ficha");

    /**
     * Borra al alumno con todo lo suyo (notas, asistencias, anotaciones, enfermería, vínculos con padres
     * y novedades pendientes) en una sola transacción: o se borra todo o no se borra nada.
     */
    public boolean delete(int alumnoId) throws SQLException {
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM alumno WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, alumnoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return false; }
                    }
                }
                for (String tabla : TABLAS_DEL_ALUMNO) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + tabla + " WHERE alumno_id = ?")) {
                        ps.setInt(1, alumnoId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM notificacion WHERE entidad_tipo = ? AND entidad_id = ?")) {
                    ps.setString(1, NotificacionDao.ENTIDAD_ALUMNO);
                    ps.setLong(2, alumnoId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM alumno WHERE id = ?")) {
                    ps.setInt(1, alumnoId);
                    if (ps.executeUpdate() != 1) { con.rollback(); return false; }
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
