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

    public boolean update(int alumnoId, String nombre, String apellido, int cursoId, String ci) throws SQLException {
        String sql = "UPDATE alumno SET ci = ?, nombre = ?, apellido = ?, curso_id = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (ci == null || ci.isBlank()) {
                ps.setNull(1, java.sql.Types.VARCHAR);
            } else {
                ps.setString(1, ci.trim());
            }
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setInt(4, cursoId);
            ps.setInt(5, alumnoId);
            return ps.executeUpdate() > 0;
        }
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

    public boolean delete(int alumnoId) throws SQLException {
        String sql = "DELETE FROM alumno WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, alumnoId);
            return ps.executeUpdate() == 1;
        }
    }
}
