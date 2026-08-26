package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.CursoBase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Repository;

@Repository
public class CursoBaseDao extends conexion {

    public List<CursoBase> findAll() throws SQLException {
        String sql = "SELECT cb.id, cb.especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                + "FROM curso_base cb "
                + "JOIN especialidad e ON e.id = cb.especialidad_id "
                + "ORDER BY e.nombre, cb.nivel DESC, cb.seccion, cb.id";
        List<CursoBase> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(fromResultSet(rs));
            }
        }
        return out;
    }

    public List<CursoBase> findAllByEspecialidadId(int especialidadId) throws SQLException {
        String sql = "SELECT cb.id, cb.especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                + "FROM curso_base cb "
                + "JOIN especialidad e ON e.id = cb.especialidad_id "
                + "WHERE cb.especialidad_id = ? "
                + "ORDER BY cb.nivel DESC, cb.seccion, cb.id";
        List<CursoBase> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(fromResultSet(rs));
                }
            }
        }
        return out;
    }

    public CursoBase findById(int id) throws SQLException {
        String sql = "SELECT cb.id, cb.especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                + "FROM curso_base cb "
                + "JOIN especialidad e ON e.id = cb.especialidad_id "
                + "WHERE cb.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? fromResultSet(rs) : null;
            }
        }
    }

    public Integer findEspecialidadId(int id) throws SQLException {
        String sql = "SELECT especialidad_id FROM curso_base WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public Integer findId(int especialidadId, int nivel, String seccion) throws SQLException {
        String sql = "SELECT id FROM curso_base WHERE especialidad_id = ? AND nivel = ? AND seccion = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            ps.setInt(2, nivel);
            ps.setString(3, seccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    public boolean exists(int especialidadId, int nivel, String seccion) throws SQLException {
        String sql = "SELECT 1 FROM curso_base WHERE especialidad_id = ? AND nivel = ? AND seccion = ? LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            ps.setInt(2, nivel);
            ps.setString(3, seccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean createIfNotExists(int especialidadId, int nivel, String seccion) throws SQLException {
        String sql = "INSERT IGNORE INTO curso_base (especialidad_id, nivel, seccion) VALUES (?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            ps.setInt(2, nivel);
            ps.setString(3, seccion);
            return ps.executeUpdate() > 0;
        }
    }

    public Set<String> listDistinctSeccionesForEspecialidad(int especialidadId) throws SQLException {
        Set<String> secciones = new HashSet<>();
        String sql = "SELECT DISTINCT seccion FROM curso_base WHERE especialidad_id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seccion = rs.getString("seccion");
                    if (seccion != null && !seccion.isBlank()) {
                        secciones.add(seccion.trim());
                    }
                }
            }
        }
        return secciones;
    }

    private CursoBase fromResultSet(ResultSet rs) throws SQLException {
        CursoBase cursoBase = new CursoBase();
        cursoBase.setId(rs.getInt("id"));
        cursoBase.setEspecialidadId(rs.getInt("especialidad_id"));
        cursoBase.setEspecialidad(rs.getString("especialidad"));
        cursoBase.setNivel(rs.getInt("nivel"));
        cursoBase.setSeccion(rs.getString("seccion"));
        return cursoBase;
    }
}
