/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ctn.informatica.sca.util.AcademicPeriod;

/**
 *
 * @author jonat
 */
@Repository
public class StudentRowDao extends conexion {
    public static boolean shouldIncludeAlumnoForPlanillaCurso(int alumnoCursoId, int planillaCursoId) {
        return alumnoCursoId == planillaCursoId;
    }

    public static int normalizeGradeValue(Integer puntos) {
        return puntos == null ? 0 : puntos;
    }

    static void addGradeIfRelevant(StudentRow row, int tareaId, Integer puntos, Map<Integer, Integer> tareaMax) {
        if (row == null || tareaMax == null || tareaId <= 0 || !tareaMax.containsKey(tareaId)) {
            return;
        }
        row.getGrades().put(tareaId, puntos);
    }

    static int sumRelevantGrades(Map<Integer, Integer> grades, Map<Integer, Integer> tareaMax) {
        if (grades == null || tareaMax == null || tareaMax.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (Integer tareaId : tareaMax.keySet()) {
            Integer puntos = grades.get(tareaId);
            sum += normalizeGradeValue(puntos);
        }
        return sum;
    }

    /**
     * Rango de fechas [desde, hasta) de una etapa del período, con el mismo criterio que
     * {@link AcademicPeriod#etapaAt(LocalDate)} usa para filtrar las tareas: la etapa 1 va
     * desde el 1 de enero hasta el inicio de la etapa 2, y la etapa 2 hasta fin de año.
     */
    static LocalDate[] etapaDateRange(int year, int etapaIndex) {
        LocalDate inicioEtapa2 = AcademicPeriod.etapaStartDate(year, 2);
        if (etapaIndex == 2) {
            return new LocalDate[]{inicioEtapa2, LocalDate.of(year + 1, 1, 1)};
        }
        return new LocalDate[]{LocalDate.of(year, 1, 1), inicioEtapa2};
    }

    private static LocalDate[] etapaDateRange(Planilla planilla) {
        int year = planilla.getPeriodo() > 0 ? planilla.getPeriodo() : AcademicPeriod.current();
        return etapaDateRange(year, planilla.getEtapaIndex());
    }

    /**
     * Cantidad de faltas RSA por alumno: cada código de conducta (fila de
     * rasgo_asistencia_codigo) asignado en las clases que el profesor de la planilla dio a ese
     * curso dentro de la etapa. Una sola consulta agrupada; los alumnos sin códigos no aparecen.
     */
    Map<Integer, Integer> countFaltasPorAlumno(Planilla planilla) throws SQLException {
        LocalDate[] rango = etapaDateRange(planilla);
        String sql = "SELECT ra.alumno_id, COUNT(rac.id) AS faltas "
                + "FROM rasgo_asistencia_codigo rac "
                + "JOIN rasgo_asistencia ra ON ra.id = rac.rasgo_asistencia_id "
                + "JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "WHERE pr.curso_id = ? AND pr.usuario_id = ? AND pr.fecha_clase >= ? AND pr.fecha_clase < ? "
                + "GROUP BY ra.alumno_id";
        Map<Integer, Integer> faltas = new HashMap<>();
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, planilla.getCursoId());
            stm.setInt(2, planilla.getProfesorId());
            stm.setDate(3, java.sql.Date.valueOf(rango[0]));
            stm.setDate(4, java.sql.Date.valueOf(rango[1]));
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    faltas.put(rs.getInt("alumno_id"), rs.getInt("faltas"));
                }
            }
        }
        return faltas;
    }

    /** Clases dadas por el profesor de la planilla a ese curso dentro de la etapa (base del % de tolerancia). */
    int countClasesDadas(Planilla planilla) throws SQLException {
        LocalDate[] rango = etapaDateRange(planilla);
        String sql = "SELECT COUNT(DISTINCT pr.id) FROM planilla_rasgo pr "
                + "WHERE pr.curso_id = ? AND pr.usuario_id = ? AND pr.fecha_clase >= ? AND pr.fecha_clase < ?";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, planilla.getCursoId());
            stm.setInt(2, planilla.getProfesorId());
            stm.setDate(3, java.sql.Date.valueOf(rango[0]));
            stm.setDate(4, java.sql.Date.valueOf(rango[1]));
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<StudentRow> loadRowsForPlanilla(Planilla planilla,
            Map<Integer, Integer> tareaMax,
            int totalPossiblePoints) throws SQLException {
        List<StudentRow> rows = new ArrayList<>();
        // RSA: solo se consultan faltas y clases dadas si la planilla lo tiene activado, y una
        // única vez para todos los alumnos (no una consulta por alumno).
        boolean rsaActivo = planilla != null && planilla.getRsaPuntos() != null;
        Map<Integer, Integer> faltasPorAlumno = rsaActivo ? countFaltasPorAlumno(planilla) : Map.of();
        int totalClasesDadas = rsaActivo ? countClasesDadas(planilla) : 0;
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
                        Integer puntos = rs.getObject("puntos", Integer.class);
                        addGradeIfRelevant(row, tareaId, puntos, tareaMax);
                    }
                }

                // compute totals & percentages for each row
                for (StudentRow r : map.values()) {
                    r.setRsaPuntos(rsaActivo
                            ? planilla.computeRsaScore(faltasPorAlumno.getOrDefault(r.getAlumnoId(), 0), totalClasesDadas)
                            : 0);
                    int sum = sumRelevantGrades(r.getGrades(), tareaMax) + r.getRsaPuntos();
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
