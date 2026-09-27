package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.AlumnoRiesgoDto;
import ctn.informatica.sca.dto.AlumnoRiesgoMateriaDto;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.ParentTaskGrade;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Alumnos "en riesgo": superan el umbral de tareas no entregadas y/o de notas conductuales,
 * sumando todas las materias del profesor autenticado en el año lectivo actual. Solo lectura,
 * todo agregado en SQL (sin N+1 por alumno).
 */
@Repository
public class AlumnoRiesgoDao extends conexion {

    private static final String MOTIVO_TAREAS = "TAREAS";
    private static final String MOTIVO_CONDUCTA = "CONDUCTA";

    public List<AlumnoRiesgoDto> listarPorProfesor(int profesorId, int anio, int umbralTareas, int umbralConducta) throws SQLException {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio + 1, 1, 1);

        try (Connection con = getCon()) {
            Map<Integer, Integer> tareasPorAlumno = contarTareasNoEntregadas(con, profesorId, desde, hasta);
            Map<Integer, Integer> conductaPorAlumno = contarNotasConductuales(con, profesorId, desde, hasta);

            Set<Integer> alumnoIds = new java.util.LinkedHashSet<>();
            alumnoIds.addAll(tareasPorAlumno.keySet());
            alumnoIds.addAll(conductaPorAlumno.keySet());
            Set<Integer> enRiesgo = alumnoIds.stream()
                    .filter(id -> tareasPorAlumno.getOrDefault(id, 0) > umbralTareas
                            || conductaPorAlumno.getOrDefault(id, 0) > umbralConducta)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            if (enRiesgo.isEmpty()) {
                return List.of();
            }

            Map<Integer, AlumnoInfo> alumnoInfoPorId = cargarAlumnoInfo(con, enRiesgo);
            Map<Integer, Map<Integer, MateriaAcc>> desglosePorAlumno = new LinkedHashMap<>();
            acumularTareasPorMateria(con, profesorId, desde, hasta, enRiesgo, desglosePorAlumno);
            acumularConductaPorMateria(con, profesorId, desde, hasta, enRiesgo, desglosePorAlumno);

            List<AlumnoRiesgoDto> resultado = new ArrayList<>();
            for (Integer alumnoId : enRiesgo) {
                AlumnoInfo info = alumnoInfoPorId.get(alumnoId);
                if (info == null) {
                    continue; // alumno sin curso resoluble (dato inconsistente): se omite, no se rompe la lista
                }
                int tareas = tareasPorAlumno.getOrDefault(alumnoId, 0);
                int conducta = conductaPorAlumno.getOrDefault(alumnoId, 0);
                List<String> motivos = new ArrayList<>();
                if (tareas > umbralTareas) motivos.add(MOTIVO_TAREAS);
                if (conducta > umbralConducta) motivos.add(MOTIVO_CONDUCTA);

                List<AlumnoRiesgoMateriaDto> desglose = new ArrayList<>();
                Map<Integer, MateriaAcc> porMateria = desglosePorAlumno.getOrDefault(alumnoId, Map.of());
                for (Map.Entry<Integer, MateriaAcc> entry : new TreeMap<>(porMateria).entrySet()) {
                    MateriaAcc acc = entry.getValue();
                    desglose.add(new AlumnoRiesgoMateriaDto(entry.getKey(), acc.nombre, acc.tareas, acc.conducta));
                }

                resultado.add(new AlumnoRiesgoDto(
                        alumnoId,
                        info.nombreCompleto,
                        info.cursoId,
                        info.cursoNombre,
                        info.especialidadId,
                        info.especialidadNombre,
                        info.seccion,
                        tareas,
                        conducta,
                        motivos,
                        desglose));
            }

            resultado.sort(Comparator.comparingInt(AlumnoRiesgoDto::cursoId)
                    .thenComparing(Comparator.comparingInt((AlumnoRiesgoDto d) -> d.tareasNoEntregadas() + d.notasConductuales()).reversed()));
            return resultado;
        }
    }

    private Map<Integer, Integer> contarTareasNoEntregadas(Connection con, int profesorId, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT r.alumno_id, COUNT(*) AS cnt "
                + "FROM planilla p "
                + "JOIN tarea t ON t.planilla_id = p.id "
                + "JOIN registro r ON r.planilla_id = p.id "
                + "LEFT JOIN puntaje pu ON pu.tarea_id = t.id AND pu.registro_id = r.id "
                + "WHERE p.usuario_id = ? AND t.fecha_limite >= ? AND t.fecha_limite < ? "
                + "AND (" + ParentTaskGrade.SQL_CONDICION_NO_ENTREGADA + ") "
                + "GROUP BY r.alumno_id";
        Map<Integer, Integer> out = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setDate(2, java.sql.Date.valueOf(desde));
            ps.setDate(3, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getInt("alumno_id"), rs.getInt("cnt"));
            }
        }
        return out;
    }

    private Map<Integer, Integer> contarNotasConductuales(Connection con, int profesorId, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT alumno_id, COUNT(*) AS cnt FROM ("
                + "SELECT ra.alumno_id FROM rasgo_asistencia ra "
                + "JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "WHERE pr.usuario_id = ? AND ra.falta_codigo IS NOT NULL "
                + "AND pr.fecha_clase >= ? AND pr.fecha_clase < ? "
                + "UNION ALL "
                + "SELECT ra.alumno_id FROM rasgo_asistencia_codigo rac "
                + "JOIN rasgo_asistencia ra ON ra.id = rac.rasgo_asistencia_id "
                + "JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "WHERE pr.usuario_id = ? "
                + "AND pr.fecha_clase >= ? AND pr.fecha_clase < ? "
                + ") u GROUP BY alumno_id";
        Map<Integer, Integer> out = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setDate(2, java.sql.Date.valueOf(desde));
            ps.setDate(3, java.sql.Date.valueOf(hasta));
            ps.setInt(4, profesorId);
            ps.setDate(5, java.sql.Date.valueOf(desde));
            ps.setDate(6, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getInt("alumno_id"), rs.getInt("cnt"));
            }
        }
        return out;
    }

    private void acumularTareasPorMateria(Connection con, int profesorId, LocalDate desde, LocalDate hasta,
            Set<Integer> alumnoIds, Map<Integer, Map<Integer, MateriaAcc>> desglosePorAlumno) throws SQLException {
        String sql = "SELECT r.alumno_id, p.materia_id, m.nombre AS materia_nombre, COUNT(*) AS cnt "
                + "FROM planilla p "
                + "JOIN materia m ON m.id = p.materia_id "
                + "JOIN tarea t ON t.planilla_id = p.id "
                + "JOIN registro r ON r.planilla_id = p.id "
                + "LEFT JOIN puntaje pu ON pu.tarea_id = t.id AND pu.registro_id = r.id "
                + "WHERE p.usuario_id = ? AND t.fecha_limite >= ? AND t.fecha_limite < ? "
                + "AND r.alumno_id IN (" + placeholders(alumnoIds.size()) + ") "
                + "AND (" + ParentTaskGrade.SQL_CONDICION_NO_ENTREGADA + ") "
                + "GROUP BY r.alumno_id, p.materia_id, m.nombre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, profesorId);
            ps.setDate(idx++, java.sql.Date.valueOf(desde));
            ps.setDate(idx++, java.sql.Date.valueOf(hasta));
            for (Integer alumnoId : alumnoIds) ps.setInt(idx++, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MateriaAcc acc = acc(desglosePorAlumno, rs.getInt("alumno_id"), rs.getInt("materia_id"), rs.getString("materia_nombre"));
                    acc.tareas += rs.getInt("cnt");
                }
            }
        }
    }

    private void acumularConductaPorMateria(Connection con, int profesorId, LocalDate desde, LocalDate hasta,
            Set<Integer> alumnoIds, Map<Integer, Map<Integer, MateriaAcc>> desglosePorAlumno) throws SQLException {
        String filtroAlumnos = "AND ra.alumno_id IN (" + placeholders(alumnoIds.size()) + ") ";
        String sql = "SELECT alumno_id, materia_id, materia_nombre, COUNT(*) AS cnt FROM ("
                + "SELECT ra.alumno_id AS alumno_id, a.materia_id AS materia_id, m.nombre AS materia_nombre "
                + "FROM rasgo_asistencia ra "
                + "JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "LEFT JOIN asignacion a ON a.id = pr.asignacion_id "
                + "LEFT JOIN materia m ON m.id = a.materia_id "
                + "WHERE pr.usuario_id = ? AND ra.falta_codigo IS NOT NULL "
                + "AND pr.fecha_clase >= ? AND pr.fecha_clase < ? " + filtroAlumnos
                + "UNION ALL "
                + "SELECT ra.alumno_id AS alumno_id, a.materia_id AS materia_id, m.nombre AS materia_nombre "
                + "FROM rasgo_asistencia_codigo rac "
                + "JOIN rasgo_asistencia ra ON ra.id = rac.rasgo_asistencia_id "
                + "JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "LEFT JOIN asignacion a ON a.id = pr.asignacion_id "
                + "LEFT JOIN materia m ON m.id = a.materia_id "
                + "WHERE pr.usuario_id = ? "
                + "AND pr.fecha_clase >= ? AND pr.fecha_clase < ? " + filtroAlumnos
                + ") u GROUP BY alumno_id, materia_id, materia_nombre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, profesorId);
            ps.setDate(idx++, java.sql.Date.valueOf(desde));
            ps.setDate(idx++, java.sql.Date.valueOf(hasta));
            for (Integer alumnoId : alumnoIds) ps.setInt(idx++, alumnoId);
            ps.setInt(idx++, profesorId);
            ps.setDate(idx++, java.sql.Date.valueOf(desde));
            ps.setDate(idx++, java.sql.Date.valueOf(hasta));
            for (Integer alumnoId : alumnoIds) ps.setInt(idx++, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer materiaIdObj = (Integer) rs.getObject("materia_id");
                    int materiaId = materiaIdObj == null ? 0 : materiaIdObj;
                    String materiaNombre = materiaIdObj == null ? "Sin materia asociada" : rs.getString("materia_nombre");
                    MateriaAcc acc = acc(desglosePorAlumno, rs.getInt("alumno_id"), materiaId, materiaNombre);
                    acc.conducta += rs.getInt("cnt");
                }
            }
        }
    }

    private Map<Integer, AlumnoInfo> cargarAlumnoInfo(Connection con, Set<Integer> alumnoIds) throws SQLException {
        String sql = "SELECT al.id AS alumno_id, al.nombre, al.apellido, al.curso_id, "
                + "c.promocion, c.seccion, e.id AS especialidad_id, e.nombre AS especialidad_nombre "
                + "FROM alumno al "
                + "JOIN curso c ON c.id = al.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE al.id IN (" + placeholders(alumnoIds.size()) + ")";
        Map<Integer, AlumnoInfo> out = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            for (Integer alumnoId : alumnoIds) ps.setInt(idx++, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cursoId = rs.getInt("curso_id");
                    String especialidadNombre = rs.getString("especialidad_nombre");
                    int promocion = rs.getInt("promocion");
                    String seccion = rs.getString("seccion");
                    Curso curso = new Curso(cursoId, especialidadNombre, promocion, seccion);
                    AlumnoInfo info = new AlumnoInfo();
                    info.nombreCompleto = rs.getString("apellido") + ", " + rs.getString("nombre");
                    info.cursoId = cursoId;
                    info.cursoNombre = curso.getCursoOrdinal();
                    info.especialidadId = rs.getInt("especialidad_id");
                    info.especialidadNombre = especialidadNombre;
                    info.seccion = seccion;
                    out.put(rs.getInt("alumno_id"), info);
                }
            }
        }
        return out;
    }

    private static MateriaAcc acc(Map<Integer, Map<Integer, MateriaAcc>> desglosePorAlumno, int alumnoId, int materiaId, String materiaNombre) {
        return desglosePorAlumno
                .computeIfAbsent(alumnoId, id -> new LinkedHashMap<>())
                .computeIfAbsent(materiaId, id -> new MateriaAcc(materiaNombre));
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private static final class AlumnoInfo {
        String nombreCompleto;
        int cursoId;
        String cursoNombre;
        int especialidadId;
        String especialidadNombre;
        String seccion;
    }

    private static final class MateriaAcc {
        final String nombre;
        int tareas;
        int conducta;

        MateriaAcc(String nombre) {
            this.nombre = nombre;
        }
    }
}
