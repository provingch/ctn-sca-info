/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Curso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author jonat
 */
@Repository
public class CursoDao extends conexion {

    public static boolean shouldIncludeCurso(int cursoId, int especialidadId,
            Set<Integer> planillaCourseIds, Set<Integer> teacherEspecialidadIds) {
        if (planillaCourseIds != null && planillaCourseIds.contains(cursoId)) {
            return true;
        }
        return teacherEspecialidadIds != null && teacherEspecialidadIds.contains(especialidadId);
    }

    public ArrayList<Curso> consultarCursos(int userId) throws SQLException {
        ArrayList<Curso> cursos = new ArrayList<>();
        int period = ctn.informatica.sca.util.AcademicPeriod.current();
        String sql = "SELECT DISTINCT c.id, e.nombre AS especialidad, c.promocion, c.seccion "
            + "FROM curso c "
                + "JOIN especialidad e ON c.especialidad_id = e.id "
            + "WHERE ( "
                + "    c.id IN (SELECT DISTINCT p.curso_id FROM planilla p WHERE p.usuario_id = ?) "
                + "   OR c.especialidad_id IN ( "
                + "       SELECT DISTINCT me.especialidad_id "
                + "       FROM planilla p "
                + "       JOIN materia_especialidad me ON me.materia_id = p.materia_id "
                + "       WHERE p.usuario_id = ? "
                + "   ) "
                + "   OR c.especialidad_id IN ( "
                + "       SELECT DISTINCT me.especialidad_id "
                + "       FROM usuario_materia um "
                + "       JOIN materia_especialidad me ON me.materia_id = um.materia_id "
                + "       WHERE um.usuario_id = ? "
                + "   ) "
                + "   OR c.id IN (SELECT curso_id FROM asignacion WHERE usuario_id = ?) "
            + ") "
            + "AND c.promocion >= ? "
            + "ORDER BY e.nombre, c.promocion, c.seccion";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, userId);
            stm.setInt(2, userId);
            stm.setInt(3, userId);
            stm.setInt(4, userId);
            stm.setInt(5, period);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int curso_id = rs.getInt("id");
                String especialidad = rs.getString("especialidad");
                int promocion = rs.getInt("promocion");
                String seccion = rs.getString("seccion");

                Curso c = new Curso(curso_id, especialidad, promocion, seccion);
                cursos.add(c);
            }
        }
        return cursos;
    }

    public Curso findById(int id) throws SQLException {
        String sql = "SELECT c.id, nombre AS especialidad, promocion, seccion "
                + "FROM curso c JOIN especialidad e ON c.especialidad_id = e.id "
                + "WHERE c.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return fromResultSet(rs);
            }
        }
    }

    public int findEspecialidadId(int id) throws SQLException {
        String sql = "SELECT especialidad_id FROM curso WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1;
                return rs.getInt(1);
            }
        }
    }

    public Curso fromResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            int curso_id = rs.getInt("id");
            String especialidad = rs.getString("especialidad");
            int promocion = rs.getInt("promocion");
            String seccion = rs.getString("seccion");

            Curso c = new Curso(curso_id, especialidad, promocion, seccion);
            return c;
        } else {
            return null;
        }
    }

    public ArrayList<Curso> findAll() throws SQLException {
        ArrayList<Curso> cursos = new ArrayList<>();
        int period = ctn.informatica.sca.util.AcademicPeriod.current();
        String sql = "SELECT c.id, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM curso c JOIN especialidad e ON c.especialidad_id = e.id "
                + "WHERE c.promocion >= ? "
                + "ORDER BY e.nombre, c.promocion, c.seccion";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, period);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    int curso_id = rs.getInt("id");
                    String especialidad = rs.getString("especialidad");
                    int promocion = rs.getInt("promocion");
                    String seccion = rs.getString("seccion");
                    Curso c = new Curso(curso_id, especialidad, promocion, seccion);
                    cursos.add(c);
                }
            }
        }
        return cursos;
    }

    public Set<String> listDistinctSeccionesForEspecialidad(int especialidadId) throws SQLException {
        Set<String> secciones = new HashSet<>();
        String sql = "SELECT DISTINCT seccion FROM curso WHERE especialidad_id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString("seccion");
                    if (s != null && !s.isBlank()) secciones.add(s.trim());
                }
            }
        }
        return secciones;
    }

    public boolean existsCurso(int especialidadId, int promocion, String seccion) throws SQLException {
        String sql = "SELECT 1 FROM curso WHERE especialidad_id = ? AND promocion = ? AND seccion = ? LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            ps.setInt(2, promocion);
            ps.setString(3, seccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean createCursoIfNotExists(int especialidadId, int promocion, String seccion) throws SQLException {
        String sql = "INSERT IGNORE INTO curso (especialidad_id, promocion, seccion) VALUES (?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, especialidadId);
            ps.setInt(2, promocion);
            ps.setString(3, seccion == null ? "" : seccion);
            return ps.executeUpdate() > 0;
        }
    }
}
