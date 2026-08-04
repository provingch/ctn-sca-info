package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import ctn.informatica.sca.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class PadreDao extends conexion {

    public Padre findById(int id) throws SQLException {
        String sql = "SELECT id, ci, nombre, apellido, usuario, contrasenia, correo, telefono, totp_secret FROM padre WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Padre padre = new Padre();
                    padre.setId(rs.getInt("id"));
                    padre.setCi(rs.getObject("ci") == null ? null : rs.getInt("ci"));
                    padre.setNombre(rs.getString("nombre"));
                    padre.setApellido(rs.getString("apellido"));
                    padre.setUsuario(rs.getString("usuario"));
                    padre.setContrasenia(rs.getString("contrasenia"));
                    padre.setCorreo(rs.getString("correo"));
                    padre.setTelefono(rs.getString("telefono"));
                    padre.setTotpSecret(rs.getString("totp_secret"));
                    return padre;
                }
            }
        }
        return null;
    }

    public boolean update(Padre padre) throws SQLException {
        String sql = "UPDATE padre SET ci = ?, nombre = ?, apellido = ?, usuario = ?, contrasenia = ?, telefono = ?, correo = ?, totp_secret = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (padre.getCi() != null) {
                ps.setInt(1, padre.getCi());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, padre.getNombre());
            ps.setString(3, padre.getApellido());
            ps.setString(4, padre.getUsuario());
            String password = padre.getContrasenia();
            if (password == null || password.trim().isEmpty()) {
                password = "password";
            }
            if (!PasswordUtil.isBcryptHash(password)) {
                password = PasswordUtil.hash(password);
            }
            ps.setString(5, password);
            ps.setString(6, padre.getTelefono());
            ps.setString(7, padre.getCorreo());
            if (padre.getTotpSecret() != null && !padre.getTotpSecret().isBlank()) {
                ps.setString(8, padre.getTotpSecret());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            ps.setInt(9, padre.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean resetPassword(int id, String newPasswordPlainText) throws SQLException {
        String sql = "UPDATE padre SET contrasenia = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            String plain = (newPasswordPlainText == null || newPasswordPlainText.trim().isEmpty())
                ? "password" : newPasswordPlainText;
            ps.setString(1, PasswordUtil.hash(plain));
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Alumno> findChildrenByPadreId(int padreId) throws SQLException {
        String sql = "SELECT a.id, a.nombre, a.apellido, a.curso_id, e.nombre AS especialidad_nombre "
                + "FROM alumno_padre ap "
                + "JOIN alumno a ON a.id = ap.alumno_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE ap.padre_id = ? "
                + "ORDER BY a.apellido, a.nombre";
        List<Alumno> alumnos = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, padreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumno alumno = new Alumno();
                    alumno.setId(rs.getInt("id"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setApellido(rs.getString("apellido"));
                    alumno.setCursoId(rs.getInt("curso_id"));
                    alumno.setEspecialidadNombre(rs.getString("especialidad_nombre"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    public List<ParentSummaryItem> findParentSummary(int padreId) throws SQLException {
        String sql = "SELECT a.id AS alumno_id, a.nombre AS alumno_nombre, a.apellido AS alumno_apellido, "
                + "c.id AS curso_id, e.nombre AS especialidad_nombre, m.id AS materia_id, m.nombre AS materia_nombre, p.id AS planilla_id, "
                + "COALESCE(SUM(CASE WHEN puntaje.puntos IS NULL THEN 0 ELSE puntaje.puntos END), 0) AS puntos, "
                + "COALESCE((SELECT SUM(t2.total) FROM tarea t2 WHERE t2.planilla_id = p.id), 0) AS total_posible, "
                + "COUNT(DISTINCT t.id) AS tareas_count "
                + "FROM alumno_padre ap "
                + "JOIN alumno a ON a.id = ap.alumno_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "JOIN planilla p ON p.curso_id = c.id "
                + "JOIN materia m ON m.id = p.materia_id "
                + "LEFT JOIN registro r ON r.planilla_id = p.id AND r.alumno_id = a.id "
                + "LEFT JOIN tarea t ON t.planilla_id = p.id "
                + "LEFT JOIN puntaje ON puntaje.tarea_id = t.id AND puntaje.registro_id = r.id "
                + "WHERE ap.padre_id = ? "
                + "GROUP BY a.id, a.nombre, a.apellido, c.id, e.nombre, m.id, m.nombre, p.id "
                + "ORDER BY e.nombre, a.apellido, a.nombre, m.nombre";

        Map<String, ParentSummaryItem> map = new LinkedHashMap<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, padreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getInt("alumno_id") + "-" + rs.getInt("materia_id") + "-" + rs.getInt("planilla_id");
                    ParentSummaryItem item = map.get(key);
                    if (item == null) {
                        item = new ParentSummaryItem();
                        item.setAlumnoId(rs.getInt("alumno_id"));
                        item.setAlumnoNombre(rs.getString("alumno_apellido") + ", " + rs.getString("alumno_nombre"));
                        item.setCursoId(rs.getInt("curso_id"));
                        item.setMateriaId(rs.getInt("materia_id"));
                        item.setMateriaNombre(rs.getString("materia_nombre"));
                        item.setPlanillaId(rs.getInt("planilla_id"));
                        item.setEspecialidadNombre(rs.getString("especialidad_nombre"));
                        map.put(key, item);
                    }
                    item.setPuntos(rs.getInt("puntos"));
                    item.setTotalPosible(rs.getInt("total_posible"));
                    item.setTareasCount(rs.getInt("tareas_count"));
                    item.recomputeDerivedValues();
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<ParentTaskGrade> findTaskGradesForAlumnoPlanilla(int alumnoId, int planillaId) throws SQLException {
        String sql = "SELECT t.id AS tarea_id, t.titulo, t.fecha, t.total, COALESCE(puntaje.puntos, 0) AS puntos "
                + "FROM tarea t "
                + "LEFT JOIN registro r ON r.planilla_id = t.planilla_id AND r.alumno_id = ? "
                + "LEFT JOIN puntaje ON puntaje.tarea_id = t.id AND puntaje.registro_id = r.id "
                + "WHERE t.planilla_id = ? "
                + "ORDER BY t.fecha, t.id";
        List<ParentTaskGrade> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, alumnoId);
            ps.setInt(2, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParentTaskGrade task = new ParentTaskGrade();
                    task.setTareaId(rs.getInt("tarea_id"));
                    task.setTareaTitulo(rs.getString("titulo"));
                    task.setFecha(rs.getObject("fecha", java.time.LocalDate.class));
                    task.setTotal(rs.getInt("total"));
                    task.setPuntos(rs.getInt("puntos"));
                    task.setPlanillaId(planillaId);
                    out.add(task);
                }
            }
        }
        return out;
    }

    public List<ParentTaskGrade> findTaskGradesForAlumno(int alumnoId) throws SQLException {
        String sql = "SELECT t.id AS tarea_id, t.titulo, t.fecha, t.total, t.planilla_id, "
                + "COALESCE(puntaje.puntos, 0) AS puntos "
                + "FROM tarea t "
                + "LEFT JOIN registro r ON r.planilla_id = t.planilla_id AND r.alumno_id = ? "
                + "LEFT JOIN puntaje ON puntaje.tarea_id = t.id AND puntaje.registro_id = r.id "
                + "ORDER BY t.planilla_id, t.fecha, t.id";
        List<ParentTaskGrade> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParentTaskGrade task = new ParentTaskGrade();
                    task.setTareaId(rs.getInt("tarea_id"));
                    task.setTareaTitulo(rs.getString("titulo"));
                    task.setFecha(rs.getObject("fecha", java.time.LocalDate.class));
                    task.setTotal(rs.getInt("total"));
                    task.setPuntos(rs.getInt("puntos"));
                    task.setPlanillaId(rs.getInt("planilla_id"));
                    out.add(task);
                }
            }
        }
        return out;
    }
}
