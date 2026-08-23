package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Asignacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AsignacionDao extends conexion {

    public List<Asignacion> findAll() throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_id, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM asignacion a "
                + "JOIN usuario u ON u.id = a.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "ORDER BY u.apellido, m.nombre, e.nombre, c.promocion, c.seccion";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asignacion a = new Asignacion();
                a.setId(rs.getInt("id"));
                a.setProfesorId(rs.getInt("profesor_id"));
                a.setMateriaId(rs.getInt("materia_id"));
                a.setCursoId(rs.getInt("curso_id"));
                String profName = rs.getString("profesor_nombre");
                String profLast = rs.getString("profesor_apellido");
                a.setProfesorNombre((profLast == null ? "" : profLast) + (profName == null ? "" : (profName.isBlank() ? "" : (" " + profName))));
                a.setMateriaNombre(rs.getString("materia_nombre"));
                String especialidad = rs.getString("especialidad");
                int promocion = rs.getInt("promocion");
                String seccion = rs.getString("seccion");
                String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                a.setCursoDescripcion(cursoDesc);
                a.setEspecialidad(especialidad);
                a.setCursoNivel(promocion);
                a.setCursoSeccion(seccion);
                out.add(a);
            }
        }
        return out;
    }

    public Asignacion findById(int id) throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_id, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM asignacion a "
                + "JOIN usuario u ON u.id = a.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE a.id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Asignacion a = new Asignacion();
                a.setId(rs.getInt("id"));
                a.setProfesorId(rs.getInt("profesor_id"));
                a.setMateriaId(rs.getInt("materia_id"));
                a.setCursoId(rs.getInt("curso_id"));
                String profName = rs.getString("profesor_nombre");
                String profLast = rs.getString("profesor_apellido");
                a.setProfesorNombre((profLast == null ? "" : profLast) + (profName == null ? "" : (profName.isBlank() ? "" : (" " + profName))));
                a.setMateriaNombre(rs.getString("materia_nombre"));
                String especialidad = rs.getString("especialidad");
                int promocion = rs.getInt("promocion");
                String seccion = rs.getString("seccion");
                String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                a.setCursoDescripcion(cursoDesc);
                a.setEspecialidad(especialidad);
                a.setCursoNivel(promocion);
                a.setCursoSeccion(seccion);
                return a;
            }
        }
    }

    public boolean existe(int profesorId, int materiaId, int cursoId) throws SQLException {
        String sql = "SELECT 1 FROM asignacion WHERE usuario_id = ? AND materia_id = ? AND curso_id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, materiaId);
            ps.setInt(3, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean actualizar(int id, int materiaId, int cursoId) throws SQLException {
        String sql = "UPDATE asignacion SET materia_id = ?, curso_id = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            ps.setInt(2, cursoId);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Asignacion> findByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_id, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre, e.nombre, c.promocion, c.seccion";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignacion a = new Asignacion();
                    a.setId(rs.getInt("id"));
                    a.setProfesorId(rs.getInt("profesor_id"));
                    a.setMateriaId(rs.getInt("materia_id"));
                    a.setCursoId(rs.getInt("curso_id"));
                    a.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    int promocion = rs.getInt("promocion");
                    String seccion = rs.getString("seccion");
                    String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                    a.setCursoDescripcion(cursoDesc);
                    a.setEspecialidad(especialidad);
                    a.setCursoNivel(promocion);
                    a.setCursoSeccion(seccion);
                    out.add(a);
                }
            }
        }
        return out;
    }

    public List<Asignacion> findByProfesorAndCurso(int profesorId, int cursoId) throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_id, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE a.usuario_id = ? AND a.curso_id = ? "
                + "ORDER BY m.nombre, e.nombre";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignacion a = new Asignacion();
                    a.setId(rs.getInt("id"));
                    a.setProfesorId(rs.getInt("profesor_id"));
                    a.setMateriaId(rs.getInt("materia_id"));
                    a.setCursoId(rs.getInt("curso_id"));
                    a.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    int promocion = rs.getInt("promocion");
                    String seccion = rs.getString("seccion");
                    String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                    a.setCursoDescripcion(cursoDesc);
                    a.setEspecialidad(especialidad);
                    a.setCursoNivel(promocion);
                    a.setCursoSeccion(seccion);
                    out.add(a);
                }
            }
        }
        return out;
    }

    public int crear(int profesorId, int materiaId, int cursoId) throws SQLException {
        if (existe(profesorId, materiaId, cursoId)) return -1;
        String sql = "INSERT INTO asignacion (usuario_id, materia_id, curso_id) VALUES (?, ?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, materiaId);
            ps.setInt(3, cursoId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM asignacion WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

}
