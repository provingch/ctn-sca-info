package ctn.informatica.sca.integration.gema.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Lectura de calificaciones para GEMA (Fase 3), acotada a tareas de origen GEMA
 * ({@code tarea.gema_tarea_id IS NOT NULL}). No se reutiliza {@code StudentRowDao#loadRowsForPlanilla}:
 * ese método inicializa toda nota ausente en 0, y GEMA necesita distinguir "sin entregar" (null) de "0 puntos".
 */
@Repository
public class GemaGradeDao extends conexion {

    public record GradeRow(int alumnoId, String alumnoCi, String gemaTareaId, Integer puntos) {
    }

    public List<GradeRow> findGradesForPlanilla(int planillaId) throws SQLException {
        String sql = "SELECT a.id AS alumno_id, a.ci AS alumno_ci, t.gema_tarea_id, p.puntos "
                + "FROM registro r "
                + "JOIN alumno a ON a.id = r.alumno_id "
                + "JOIN tarea t ON t.planilla_id = r.planilla_id AND t.gema_tarea_id IS NOT NULL "
                + "LEFT JOIN puntaje p ON p.registro_id = r.id AND p.tarea_id = t.id "
                + "WHERE r.planilla_id = ? "
                + "ORDER BY a.apellido, a.nombre, t.gema_tarea_id";
        List<GradeRow> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new GradeRow(rs.getInt("alumno_id"), rs.getString("alumno_ci"),
                            rs.getString("gema_tarea_id"), rs.getObject("puntos", Integer.class)));
                }
            }
        }
        return out;
    }
}
