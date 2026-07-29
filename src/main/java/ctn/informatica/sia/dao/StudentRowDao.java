/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.Planilla;
import ctn.informatica.sia.model.StudentRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jonat
 */
public class StudentRowDao extends conexion {
    public static boolean shouldIncludeAlumnoForPlanillaCurso(int alumnoCursoId, int planillaCursoId) {
        return alumnoCursoId == planillaCursoId;
    }

    public static int normalizeGradeValue(Integer puntos) {
        return puntos == null ? 0 : puntos;
    }

    public List<StudentRow> loadRowsForPlanilla(Planilla planilla,
            Map<Integer, Integer> tareaMax,
            int totalPossiblePoints) throws SQLException {
        List<StudentRow> rows = new ArrayList<>();
        // SQL: get registros (students) and any puntaje (left join)
        // We join registro -> alumno and left join puntaje (to get tarea_id and puntos)
        String sql = "SELECT r.id AS registro_id, a.id AS alumno_id, a.nombre, a.apellido, a.curso_id, "
                + "p.tarea_id, p.puntos "
                + "FROM registro r "
                + "JOIN alumno a ON r.alumno_id = a.id "
                + "LEFT JOIN puntaje p ON p.registro_id = r.id "
                + "WHERE r.planilla_id = ? "
                + "ORDER BY a.apellido, a.nombre, r.id";
        // We'll group by registro_id
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, planilla.getId());
            try (ResultSet rs = stm.executeQuery()) {
                Map<Integer, StudentRow> map = new LinkedHashMap<>(); // keep order
                
                while (rs.next()) {
                    int registroId = rs.getInt("registro_id");
                    int alumnoCursoId = rs.getInt("curso_id");
                    if (!shouldIncludeAlumnoForPlanillaCurso(alumnoCursoId, planilla.getCursoId())) {
                        continue;
                    }
                    StudentRow row = map.get(registroId);
                    if (row == null) {
                        row = new StudentRow();
                        row.setRegistroId(registroId);
                        row.setAlumnoId(rs.getInt("alumno_id"));
                        String nombre = rs.getString("nombre");
                        String apellido = rs.getString("apellido");
                        row.setAlumnoNombre((apellido == null ? "" : apellido) + ", " + (nombre == null ? "" : nombre));
                        
                        // Initialize grades map with all tareas -> 0 so missing submissions are treated as zero
                        for (Integer tareaId : tareaMax.keySet()) {
                            row.getGrades().put(tareaId, 0);
                        }
                        
                        map.put(registroId, row);
                    }

                    // tarea_id may be NULL due to LEFT JOIN; getInt + wasNull works but we'll use getObject for puntos
                    int tareaId = rs.getInt("tarea_id");
                    if (!rs.wasNull() && tareaId > 0) {
                        // use getObject to obtain an Integer that is null for SQL NULL
                        Integer puntos = rs.getObject("puntos", Integer.class);
                        // Put the puntos value (may be null) into the map
                        row.getGrades().put(tareaId, puntos);
                    }
                }

                // compute totals & percentages for each row
                for (StudentRow r : map.values()) {
                    int sum = 0;
                    for (Map.Entry<Integer, Integer> e : r.getGrades().entrySet()) {
                        Integer puntos = e.getValue();
                        sum += normalizeGradeValue(puntos);
                    }
                    r.setTotal(sum);
                    // compute porcentaje = round(sum * 100 / totalPossiblePoints)
                    int porcentaje = 0;
                    if (totalPossiblePoints > 0) {
                        double raw = (sum * 100.0) / totalPossiblePoints;
                        porcentaje = (int) Math.round(raw);
                    }
                    r.setPorcentaje(porcentaje);
                    
                    int nota;
                    if (planilla != null) {
                        // ensure ranges computed (caller should have called computeGradeRanges)
                        nota = planilla.getNotaForSum(sum);
                    } else {
                        // fallback (if Planilla not provided): use exigencia-based calculation or default
                        nota = porcentaje; // or previous fallback; adjust to your needs
                    }

                    r.setNota(nota);
                    rows.add(r);
                }
            }
        }
        return rows;
    }
}