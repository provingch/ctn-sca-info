package ctn.informatica.sca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.CursoBase;

@Repository
public class AsignacionDao extends conexion {

    public List<Asignacion> findAll() throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_base_id, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "m.nombre AS materia_nombre, e.id AS especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                + "FROM asignacion a "
                + "JOIN usuario u ON u.id = a.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                + "JOIN especialidad e ON e.id = cb.especialidad_id "
                + "ORDER BY u.apellido, m.nombre, e.nombre, cb.nivel DESC, cb.seccion";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asignacion a = new Asignacion();
                a.setId(rs.getInt("id"));
                a.setProfesorId(rs.getInt("profesor_id"));
                a.setMateriaId(rs.getInt("materia_id"));
                a.setCursoBaseId(rs.getInt("curso_base_id"));
                String profName = rs.getString("profesor_nombre");
                String profLast = rs.getString("profesor_apellido");
                a.setProfesorNombre((profLast == null ? "" : profLast) + (profName == null ? "" : (profName.isBlank() ? "" : (" " + profName))));
                a.setMateriaNombre(rs.getString("materia_nombre"));
                String especialidad = rs.getString("especialidad");
                int nivel = rs.getInt("nivel");
                String seccion = rs.getString("seccion");
                String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                a.setCursoDescripcion(cursoDesc);
                setCourseFields(a, rs.getInt("especialidad_id"), especialidad, nivel, seccion);
                out.add(a);
            }
        }
        return out;
    }

        public List<Asignacion> findByEspecialidad(int especialidadId) throws SQLException {
            String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_base_id, "
                    + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                    + "m.nombre AS materia_nombre, e.id AS especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                    + "FROM asignacion a "
                    + "JOIN usuario u ON u.id = a.usuario_id "
                    + "JOIN materia m ON m.id = a.materia_id "
                    + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                    + "JOIN especialidad e ON e.id = cb.especialidad_id "
                    + "WHERE cb.especialidad_id = ? "
                    + "ORDER BY u.apellido, m.nombre, e.nombre, cb.nivel DESC, cb.seccion";
            List<Asignacion> out = new ArrayList<>();
            try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, especialidadId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Asignacion a = new Asignacion();
                        a.setId(rs.getInt("id"));
                        a.setProfesorId(rs.getInt("profesor_id"));
                        a.setMateriaId(rs.getInt("materia_id"));
                        a.setCursoBaseId(rs.getInt("curso_base_id"));
                        String profName = rs.getString("profesor_nombre");
                        String profLast = rs.getString("profesor_apellido");
                        a.setProfesorNombre((profLast == null ? "" : profLast) + (profName == null ? "" : (profName.isBlank() ? "" : (" " + profName))));
                        a.setMateriaNombre(rs.getString("materia_nombre"));
                        String especialidad = rs.getString("especialidad");
                        int nivel = rs.getInt("nivel");
                        String seccion = rs.getString("seccion");
                        String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                        a.setCursoDescripcion(cursoDesc);
                        setCourseFields(a, rs.getInt("especialidad_id"), especialidad, nivel, seccion);
                        out.add(a);
                    }
                }
            }
            return out;
        }

    public Asignacion findById(int id) throws SQLException {
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_base_id, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "m.nombre AS materia_nombre, e.id AS especialidad_id, e.nombre AS especialidad, cb.nivel, cb.seccion "
                + "FROM asignacion a "
                + "JOIN usuario u ON u.id = a.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                + "JOIN especialidad e ON e.id = cb.especialidad_id "
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
                a.setCursoBaseId(rs.getInt("curso_base_id"));
                String profName = rs.getString("profesor_nombre");
                String profLast = rs.getString("profesor_apellido");
                a.setProfesorNombre((profLast == null ? "" : profLast) + (profName == null ? "" : (profName.isBlank() ? "" : (" " + profName))));
                a.setMateriaNombre(rs.getString("materia_nombre"));
                String especialidad = rs.getString("especialidad");
                int nivel = rs.getInt("nivel");
                String seccion = rs.getString("seccion");
                String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                a.setCursoDescripcion(cursoDesc);
                setCourseFields(a, rs.getInt("especialidad_id"), especialidad, nivel, seccion);
                return a;
            }
        }
    }

    public boolean existe(int profesorId, int materiaId, int cursoId) throws SQLException {
        String sql = "SELECT 1 FROM asignacion WHERE usuario_id = ? AND materia_id = ? AND curso_base_id = ?";
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
        String sql = "UPDATE asignacion SET materia_id = ?, curso_base_id = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            ps.setInt(2, cursoId);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Asignacion> findByProfesor(int profesorId) throws SQLException {
        // cr resuelve el curso_base (id de especialidad/nivel/sección, sin año) al curso
        // real vigente (curso.id, ligado a una promoción concreta): mismo criterio de
        // "promoción vigente" que Curso.getCurso() (promocion = period - nivel + 3), y
        // el UNIQUE(especialidad_id, promocion, seccion) de curso garantiza a lo sumo una
        // fila. Sin match (p.ej. curso_base sin curso creado todavía) queda NULL.
        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_base_id, cr.id AS curso_real_id, "
                + "m.nombre AS materia_nombre, e.id AS especialidad_id, e.nombre AS especialidad, c.nivel AS nivel, c.seccion, "
                + "COALESCE(p.estado, 'NO_CARGADO') AS plan_estado "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso_base c ON c.id = a.curso_base_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "LEFT JOIN plan_curricular p ON p.asignacion_id = a.id AND p.etapa = ? AND p.anio_lectivo = ? "
                + "LEFT JOIN curso cr ON cr.especialidad_id = c.especialidad_id AND cr.seccion = c.seccion AND cr.promocion = (? - c.nivel + 3) "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre, e.nombre, c.nivel DESC, c.seccion";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            int currentYear = LocalDate.now().getYear();
            ps.setInt(1, ctn.informatica.sca.util.AcademicPeriod.currentEtapa());
            ps.setInt(2, currentYear);
            ps.setInt(3, currentYear);
            ps.setInt(4, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignacion a = new Asignacion();
                    a.setId(rs.getInt("id"));
                    a.setProfesorId(rs.getInt("profesor_id"));
                    a.setMateriaId(rs.getInt("materia_id"));
                    a.setCursoBaseId(rs.getInt("curso_base_id"));
                    a.setCursoRealId((Integer) rs.getObject("curso_real_id"));
                    a.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    int nivel = rs.getInt("nivel");
                    String seccion = rs.getString("seccion");
                    String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                    a.setCursoDescripcion(cursoDesc);
                    setCourseFields(a, rs.getInt("especialidad_id"), especialidad, nivel, seccion);
                    a.setEstadoPlan(rs.getString("plan_estado"));
                    out.add(a);
                }
            }
        }
        return out;
    }

    public List<Asignacion> findByProfesorAndCurso(int profesorId, int cursoId) throws SQLException {
        int currentYear = LocalDate.now().getYear();
        int currentEtapa = ctn.informatica.sca.util.AcademicPeriod.currentEtapa();

        String sql = "SELECT a.id, a.usuario_id AS profesor_id, a.materia_id, a.curso_base_id, "
                + "m.nombre AS materia_nombre, e.id AS especialidad_id, e.nombre AS especialidad, c.nivel AS nivel, c.seccion, "
                + "COALESCE(p.estado, 'NO_CARGADO') AS plan_estado "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso_base c ON c.id = a.curso_base_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "LEFT JOIN plan_curricular p ON p.asignacion_id = a.id AND p.etapa = ? AND p.anio_lectivo = ? "
                + "WHERE a.usuario_id = ? AND a.curso_base_id = ? "
                + "ORDER BY m.nombre, e.nombre";
        List<Asignacion> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, currentEtapa);
            ps.setInt(2, currentYear);
            ps.setInt(3, profesorId);
            ps.setInt(4, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignacion a = new Asignacion();
                    a.setId(rs.getInt("id"));
                    a.setProfesorId(rs.getInt("profesor_id"));
                    a.setMateriaId(rs.getInt("materia_id"));
                    a.setCursoBaseId(rs.getInt("curso_base_id"));
                    a.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    int nivel = rs.getInt("nivel");
                    String seccion = rs.getString("seccion");
                    String cursoDesc = (especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                    a.setCursoDescripcion(cursoDesc);
                    setCourseFields(a, rs.getInt("especialidad_id"), especialidad, nivel, seccion);
                    a.setEstadoPlan(rs.getString("plan_estado"));
                    out.add(a);
                }
            }
        }
        return out;
    }

    public int crear(int profesorId, int materiaId, int cursoId) throws SQLException {
        if (existe(profesorId, materiaId, cursoId)) return -1;
        String sql = "INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES (?, ?, ?)";
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

    private void setCourseFields(Asignacion asignacion, int especialidadId, String especialidad,
            int nivel, String seccion) {
        asignacion.setEspecialidadId(especialidadId);
        asignacion.setEspecialidad(especialidad);
        asignacion.setCursoNivel(nivel);
        asignacion.setCursoOrdinal(new CursoBase(asignacion.getCursoBaseId(), especialidadId, especialidad, nivel, seccion).getCursoOrdinal());
        asignacion.setCursoSeccion(seccion);
    }

}
