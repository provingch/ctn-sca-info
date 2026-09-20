package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.util.NombreUtil;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.dto.UpdateRasgoAsistenciaRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

@Repository
public class RasgoPlanillaDao extends conexion {

    static String buildInsertAsistenciaSql(boolean includeFaltaCodigo, boolean includeFaltaObservacion) {
        StringBuilder sql = new StringBuilder("INSERT INTO rasgo_asistencia (planilla_rasgo_id, alumno_id, alumno_nombre, alumno_apellido, alumno_email, estado");
        if (includeFaltaCodigo) {
            sql.append(", falta_codigo");
        }
        if (includeFaltaObservacion) {
            sql.append(", falta_observacion");
        }
        sql.append(") VALUES (?, ?, ?, ?, ?, ?");
        if (includeFaltaCodigo) {
            sql.append(", ?");
        }
        if (includeFaltaObservacion) {
            sql.append(", ?");
        }
        sql.append(")");
        return sql.toString();
    }

    static String buildPlanillaListSql(boolean includeFechaClase, boolean includeCreatedAt) {
        return buildPlanillaListSql(includeFechaClase, includeCreatedAt, true);
    }

    static String buildPlanillaListSql(boolean includeFechaClase, boolean includeCreatedAt, boolean filterByProfesor) {
        StringBuilder sql = new StringBuilder("SELECT id, curso_id, usuario_id AS profesor_id, tema");
        if (includeFechaClase) {
            sql.append(", fecha_clase");
        }
        if (includeCreatedAt) {
            sql.append(", created_at");
        }
        sql.append(" FROM planilla_rasgo WHERE");
        if (filterByProfesor) {
            sql.append(" usuario_id = ? AND");
        }
        sql.append(" curso_id = ?");
        if (includeCreatedAt) {
            sql.append(" ORDER BY created_at DESC");
        } else {
            sql.append(" ORDER BY id DESC");
        }
        return sql.toString();
    }

    static String buildRespuestaUpdateSql(boolean includeFaltaCodigo, boolean includeFaltaObservacion, boolean includeRespondedAt) {
        StringBuilder sql = new StringBuilder("UPDATE rasgo_asistencia SET estado = ?");
        if (includeFaltaCodigo) {
            sql.append(", falta_codigo = ?");
        }
        if (includeFaltaObservacion) {
            sql.append(", falta_observacion = ?");
        }
        if (includeRespondedAt) {
            sql.append(", responded_at = CURRENT_TIMESTAMP");
        }
        sql.append(" WHERE id = ?");
        return sql.toString();
    }

    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, List<Alumno> alumnos) throws SQLException {
        return crearPlanillaRasgo(cursoId, profesorId, tema, alumnos, java.util.Collections.emptySet(), Collections.emptyMap());
    }

    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, List<Alumno> alumnos, Set<Integer> alumnosAusentes) throws SQLException {
        return crearPlanillaRasgo(cursoId, profesorId, tema, alumnos, alumnosAusentes, Collections.emptyMap());
    }

    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, List<Alumno> alumnos, Set<Integer> alumnosAusentes, Map<Integer, List<String>> codigosPorAlumno) throws SQLException {
        if (alumnos == null || alumnos.isEmpty()) {
            throw new SQLException("No hay alumnos elegibles para crear la planilla de rasgos");
        }

        String insertPlanillaSql = "INSERT INTO planilla_rasgo (curso_id, usuario_id, tema, fecha_clase) VALUES (?, ?, ?, CURRENT_DATE())";

        try (Connection con = getCon()) {
            boolean[] supportsFaltaColumns = supportsColumns(con, "rasgo_asistencia", "falta_codigo", "falta_observacion");
            String insertAsistenciaSql = buildInsertAsistenciaSql(supportsFaltaColumns[0], supportsFaltaColumns[1]);
            boolean originalAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int planillaId;
                try (PreparedStatement ps = con.prepareStatement(insertPlanillaSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, cursoId);
                    ps.setInt(2, profesorId);
                    ps.setString(3, tema);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se pudo generar la planilla de rasgos");
                        }
                        planillaId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(insertAsistenciaSql)) {
                    for (Alumno alumno : alumnos) {
                        String estado = alumnosAusentes != null && alumnosAusentes.contains(alumno.getId())
                                ? "ausente" : "presente";
                        ps.setInt(1, planillaId);
                        ps.setInt(2, alumno.getId());
                        ps.setString(3, alumno.getNombre());
                        ps.setString(4, alumno.getApellido());
                        String alumnoEmail = alumno.getGoogleEmail();
                        ps.setString(5, alumnoEmail == null || alumnoEmail.isBlank() ? "" : alumnoEmail);
                        ps.setString(6, estado);
                        if (supportsFaltaColumns[0]) {
                            ps.setString(7, null);
                        }
                        if (supportsFaltaColumns[1]) {
                            ps.setString(supportsFaltaColumns[0] ? 8 : 7, null);
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                guardarCodigosDeAlumnos(con, planillaId, alumnos, codigosPorAlumno);

                con.commit();
                return planillaId;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }

    // Nueva sobrecarga: permite persistir asignacion_id si se conoce
    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, List<Alumno> alumnos, Set<Integer> alumnosAusentes, Map<Integer, List<String>> codigosPorAlumno, Integer asignacionId) throws SQLException {
        return crearPlanillaRasgo(cursoId, profesorId, tema, null, alumnos, alumnosAusentes, codigosPorAlumno, asignacionId, null, null, null, null, null, null);
    }

    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, String justificacionAtraso,
            List<Alumno> alumnos, Set<Integer> alumnosAusentes, Map<Integer, List<String>> codigosPorAlumno,
            Integer asignacionId, LocalTime horaInicio, Integer horasCatedra, LocalTime horaFin,
            String modalidad, String observaciones, Integer instrumentoId) throws SQLException {
        if (alumnos == null || alumnos.isEmpty()) {
            throw new SQLException("No hay alumnos elegibles para crear la planilla de rasgos");
        }

        String insertPlanillaSql = "INSERT INTO planilla_rasgo "
                + "(curso_id, usuario_id, asignacion_id, tema, justificacion_atraso, hora_inicio, horas_catedra, hora_fin, modalidad, observaciones, instrumento_id, fecha_clase) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE())";

        try (Connection con = getCon()) {
            boolean[] supportsFaltaColumns = supportsColumns(con, "rasgo_asistencia", "falta_codigo", "falta_observacion");
            String insertAsistenciaSql = buildInsertAsistenciaSql(supportsFaltaColumns[0], supportsFaltaColumns[1]);
            boolean originalAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int planillaId;
                try (PreparedStatement ps = con.prepareStatement(insertPlanillaSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, cursoId);
                    ps.setInt(2, profesorId);
                    if (asignacionId != null) ps.setInt(3, asignacionId); else ps.setNull(3, Types.INTEGER);
                    ps.setString(4, tema);
                    if (justificacionAtraso == null || justificacionAtraso.isBlank()) {
                        ps.setNull(5, Types.VARCHAR);
                    } else {
                        ps.setString(5, justificacionAtraso.trim());
                    }
                    if (horaInicio == null) ps.setNull(6, Types.TIME); else ps.setTime(6, Time.valueOf(horaInicio));
                    if (horasCatedra == null) ps.setNull(7, Types.TINYINT); else ps.setInt(7, horasCatedra);
                    if (horaFin == null) ps.setNull(8, Types.TIME); else ps.setTime(8, Time.valueOf(horaFin));
                    if (modalidad == null || modalidad.isBlank()) ps.setNull(9, Types.VARCHAR); else ps.setString(9, modalidad);
                    if (observaciones == null || observaciones.isBlank()) ps.setNull(10, Types.VARCHAR); else ps.setString(10, observaciones.trim());
                    if (instrumentoId == null || instrumentoId <= 0) ps.setNull(11, Types.INTEGER); else ps.setInt(11, instrumentoId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo generar la planilla de rasgos");
                        planillaId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(insertAsistenciaSql)) {
                    for (Alumno alumno : alumnos) {
                        String estado = alumnosAusentes != null && alumnosAusentes.contains(alumno.getId())
                                ? "ausente" : "presente";
                        ps.setInt(1, planillaId);
                        ps.setInt(2, alumno.getId());
                        ps.setString(3, alumno.getNombre());
                        ps.setString(4, alumno.getApellido());
                        String alumnoEmail = alumno.getGoogleEmail();
                        ps.setString(5, alumnoEmail == null || alumnoEmail.isBlank() ? "" : alumnoEmail);
                        ps.setString(6, estado);
                        if (supportsFaltaColumns[0]) {
                            ps.setString(7, null);
                        }
                        if (supportsFaltaColumns[1]) {
                            ps.setString(supportsFaltaColumns[0] ? 8 : 7, null);
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                guardarCodigosDeAlumnos(con, planillaId, alumnos, codigosPorAlumno);

                con.commit();
                return planillaId;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public boolean existeClaseParaAsignacionYFecha(int asignacionId, LocalDate fecha) throws SQLException {
        String sql = "SELECT 1 FROM planilla_rasgo WHERE asignacion_id = ? AND fecha_clase = ? LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void actualizarVerificacionPlanilla(int planillaRasgoId, String estado, Integer temaPlanId) throws SQLException {
        String sql = "UPDATE planilla_rasgo SET estado_verificacion_tema = ? , tema_plan_curricular_id = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            if (temaPlanId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, temaPlanId);
            ps.setInt(3, planillaRasgoId);
            ps.executeUpdate();
        }
    }

    /**
     * Clases de la asignación que quedaron en SIN_PLAN (dadas antes de que el plan estuviera aprobado),
     * del año y de los meses indicados, en el orden en que se dieron.
     */
    public List<ctn.informatica.sca.dto.ClaseSinPlanDto> listarClasesSinPlan(int asignacionId, int anio, List<Integer> meses) throws SQLException {
        if (meses == null || meses.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(meses.size(), "?"));
        String sql = "SELECT id, usuario_id, tema, fecha_clase FROM planilla_rasgo "
                + "WHERE asignacion_id = ? AND estado_verificacion_tema = 'SIN_PLAN' AND YEAR(fecha_clase) = ? "
                + "AND MONTH(fecha_clase) IN (" + placeholders + ") ORDER BY fecha_clase ASC, id ASC";
        List<ctn.informatica.sca.dto.ClaseSinPlanDto> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, anio);
            for (int i = 0; i < meses.size(); i++) {
                ps.setInt(3 + i, meses.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ctn.informatica.sca.dto.ClaseSinPlanDto(
                            rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("tema"),
                            rs.getDate("fecha_clase").toLocalDate()));
                }
            }
        }
        return out;
    }

    private void guardarCodigosDeAlumnos(Connection con, int planillaId, List<Alumno> alumnos, Map<Integer, List<String>> codigosPorAlumno) throws SQLException {
        if (codigosPorAlumno == null || codigosPorAlumno.isEmpty()) return;
        String sql = "INSERT IGNORE INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo) "
                + "SELECT ra.id, ? FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = ? AND ra.alumno_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Alumno alumno : alumnos) {
                List<String> codigos = codigosPorAlumno.get(alumno.getId());
                if (codigos == null) continue;
                for (String codigo : validarCodigos(con, codigos)) {
                    ps.setString(1, codigo);
                    ps.setInt(2, planillaId);
                    ps.setInt(3, alumno.getId());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private boolean[] supportsColumns(Connection con, String tableName, String... columnNames) throws SQLException {
        boolean[] result = new boolean[columnNames.length];
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < columnNames.length; i++) {
                ps.setString(1, tableName);
                ps.setString(2, columnNames[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    result[i] = rs.next();
                }
            }
        }
        return result;
    }

    public List<RasgoPlanilla> listarPorProfesorCurso(int profesorId, int cursoId) throws SQLException {
        List<RasgoPlanilla> planillas = new ArrayList<>();
        try (Connection con = getCon()) {
            boolean[] supportsPlanillaColumns = supportsColumns(con, "planilla_rasgo", "fecha_clase", "created_at");
            String sql = buildPlanillaListSql(supportsPlanillaColumns[0], supportsPlanillaColumns[1]);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, profesorId);
                ps.setInt(2, cursoId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        planillas.add(fromPlanillaResultSet(rs));
                    }
                }
            }
        }
        return planillas;
    }

    /**
     * Lista todas las clases dictadas por un profesor (para la vista "Mis clases"),
     * más la información del curso, materia y contadores de ausencias.
     */
    public List<ctn.informatica.sca.dto.ClaseDadaDto> listarClasesDadasPorProfesor(int profesorId) throws SQLException {
        return listarClasesDadas("pr.usuario_id = ?", ps -> ps.setInt(1, profesorId));
    }

    /**
     * Lista todas las clases dictadas dentro de una especialidad (para admin nivel 3).
     */
    public List<ctn.informatica.sca.dto.ClaseDadaDto> listarClasesDadasPorEspecialidad(int especialidadId) throws SQLException {
        return listarClasesDadas("e.id = ?", ps -> ps.setInt(1, especialidadId));
    }

    /**
     * Lista todas las clases dictadas sin filtro (para admin global).
     */
    public List<ctn.informatica.sca.dto.ClaseDadaDto> listarClasesDadas() throws SQLException {
        return listarClasesDadas(null, ps -> {});
    }

    @FunctionalInterface
    private interface PsBinder { void bind(PreparedStatement ps) throws SQLException; }

    private List<ctn.informatica.sca.dto.ClaseDadaDto> listarClasesDadas(String whereClause, PsBinder binder) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT pr.id, pr.fecha_clase, pr.created_at, pr.tema, pr.curso_id, pr.asignacion_id, pr.usuario_id AS profesor_id, "
                + "c.promocion AS curso_promocion, c.seccion AS curso_seccion, "
                + "e.id AS especialidad_id, e.nombre AS especialidad_nombre, "
                + "m.nombre AS materia_nombre, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "(SELECT COUNT(*) FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = pr.id) AS total_alumnos, "
                + "(SELECT COUNT(*) FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = pr.id AND ra.estado = 'ausente') AS total_ausentes, "
                + "(SELECT COUNT(*) FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = pr.id AND ra.estado = 'ausente_justificado') AS total_justificados, "
                + "(SELECT COUNT(*) FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = pr.id AND ra.estado = 'presente') AS total_presentes, "
                + "(SELECT COUNT(*) FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = pr.id AND ra.estado = 'pendiente') AS total_pendientes "
                + "FROM planilla_rasgo pr "
                + "JOIN curso c ON c.id = pr.curso_id "
                + "LEFT JOIN especialidad e ON e.id = c.especialidad_id "
                + "LEFT JOIN asignacion a ON a.id = pr.asignacion_id "
                + "LEFT JOIN materia m ON m.id = a.materia_id "
                + "LEFT JOIN usuario u ON u.id = pr.usuario_id ");
        if (whereClause != null && !whereClause.isBlank()) {
            sql.append("WHERE ").append(whereClause).append(' ');
        }
        sql.append("ORDER BY pr.fecha_clase DESC, pr.created_at DESC, pr.id DESC");

        List<ctn.informatica.sca.dto.ClaseDadaDto> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seccion = rs.getString("curso_seccion");
                    Integer promocion = rs.getObject("curso_promocion", Integer.class);
                    String especialidadNombre = rs.getString("especialidad_nombre");
                    String cursoDesc = (especialidadNombre == null ? "" : especialidadNombre)
                            + (promocion == null ? "" : (" " + promocion))
                            + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                    // Pantallas de evaluación: nombre corto (primera palabra de nombre + apellido).
                    String profesorNombre = NombreUtil.corto(rs.getString("profesor_nombre"), rs.getString("profesor_apellido"));
                    java.sql.Date fecha = rs.getDate("fecha_clase");
                    out.add(new ctn.informatica.sca.dto.ClaseDadaDto(
                            rs.getInt("id"),
                            fecha == null ? null : fecha.toString(),
                            rs.getString("tema"),
                            rs.getInt("curso_id"),
                            cursoDesc.trim(),
                            rs.getObject("asignacion_id", Integer.class),
                            rs.getString("materia_nombre"),
                            rs.getInt("profesor_id"),
                            profesorNombre.isBlank() ? null : profesorNombre,
                            rs.getObject("especialidad_id", Integer.class),
                            especialidadNombre,
                            rs.getInt("total_alumnos"),
                            rs.getInt("total_ausentes"),
                            rs.getInt("total_justificados"),
                            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                            rs.getInt("total_presentes"),
                            rs.getInt("total_pendientes")));
                }
            }
        }
        return out;
    }

    public Integer findEspecialidadIdByPlanilla(int planillaId) throws SQLException {
        String sql = "SELECT c.especialidad_id FROM planilla_rasgo pr "
                + "JOIN curso c ON c.id = pr.curso_id WHERE pr.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1, Integer.class);
                }
            }
        }
        return null;
    }

    public List<RasgoPlanilla> listarPorCurso(int cursoId) throws SQLException {
        List<RasgoPlanilla> planillas = new ArrayList<>();
        try (Connection con = getCon()) {
            boolean[] supportsPlanillaColumns = supportsColumns(con, "planilla_rasgo", "fecha_clase", "created_at");
            String sql = buildPlanillaListSql(supportsPlanillaColumns[0], supportsPlanillaColumns[1], false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, cursoId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        planillas.add(fromPlanillaResultSet(rs));
                    }
                }
            }
        }
        return planillas;
    }

    public RasgoPlanilla findPlanillaById(int planillaId) throws SQLException {
        String sql = "SELECT id, curso_id, usuario_id AS profesor_id, tema, fecha_clase, created_at FROM planilla_rasgo WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromPlanillaResultSet(rs);
                }
            }
        }
        return null;
    }

    public Integer findTemaPlanIdByPlanillaId(int planillaId) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("SELECT tema_plan_curricular_id FROM planilla_rasgo WHERE id = ?")) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("tema_plan_curricular_id", Integer.class);
                }
            }
        }
        return null;
    }

    public java.util.List<ctn.informatica.sca.dto.VerificacionDudosaDto> listarVerificacionesDudosas() throws SQLException {
        String sql = "SELECT pr.id AS planilla_id, pr.curso_id, pr.asignacion_id, m.nombre AS materia_nombre, u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, pr.tema AS tema_ingresado, t.id AS tema_plan_id, t.temas_contenidos AS tema_esperado, pr.fecha_clase " +
                "FROM planilla_rasgo pr " +
                "LEFT JOIN asignacion a ON a.id = pr.asignacion_id " +
                "LEFT JOIN materia m ON m.id = a.materia_id " +
                "LEFT JOIN usuario u ON u.id = pr.usuario_id " +
                "LEFT JOIN tema_plan_curricular t ON t.id = pr.tema_plan_curricular_id " +
                "WHERE pr.estado_verificacion_tema = 'DUDOSO' " +
                "ORDER BY pr.created_at DESC";
        java.util.List<ctn.informatica.sca.dto.VerificacionDudosaDto> result = new java.util.ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int planillaId = rs.getInt("planilla_id");
                int cursoId = rs.getInt("curso_id");
                Integer asignacionId = rs.getObject("asignacion_id", Integer.class);
                String materiaNombre = rs.getString("materia_nombre");
                String profesorNombre = NombreUtil.corto(rs.getString("profesor_nombre"), rs.getString("profesor_apellido"));
                String temaIngresado = rs.getString("tema_ingresado");
                Integer temaPlanId = rs.getObject("tema_plan_id", Integer.class);
                String temaEsperado = rs.getString("tema_esperado");
                String fechaClase = rs.getString("fecha_clase");
                result.add(new ctn.informatica.sca.dto.VerificacionDudosaDto(planillaId, cursoId, asignacionId, materiaNombre, profesorNombre, temaIngresado, temaPlanId, temaEsperado, fechaClase));
            }
        }
        return result;
    }

    public List<RasgoAsistencia> listarAsistencias(int planillaRasgoId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema, cc.descripcion AS falta_codigo_descripcion "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "LEFT JOIN codigo_conducta cc ON cc.codigo = ra.falta_codigo "
                + "WHERE ra.planilla_rasgo_id = ? ORDER BY ra.alumno_apellido, ra.alumno_nombre";
        List<RasgoAsistencia> asistencias = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaRasgoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RasgoAsistencia asistencia = fromAsistenciaResultSet(rs);
                    asistencia.setCodigos(listarCodigos(con, asistencia.getId()));
                    asistencias.add(asistencia);
                }
            }
        }
        return asistencias;
    }

    public RasgoAsistencia findAsistenciaById(int asistenciaId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema, cc.descripcion AS falta_codigo_descripcion "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "LEFT JOIN codigo_conducta cc ON cc.codigo = ra.falta_codigo "
                + "WHERE ra.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asistenciaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromAsistenciaResultSet(rs);
                }
            }
        }
        return null;
    }

    public RasgoAsistencia findAsistenciaByPlanillaAndAlumno(int planillaRasgoId, int alumnoId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema, cc.descripcion AS falta_codigo_descripcion "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "LEFT JOIN codigo_conducta cc ON cc.codigo = ra.falta_codigo "
                + "WHERE ra.planilla_rasgo_id = ? AND ra.alumno_id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaRasgoId);
            ps.setInt(2, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromAsistenciaResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean registrarRespuesta(int asistenciaId, String estado) throws SQLException {
        return registrarRespuesta(asistenciaId, estado, null, null);
    }

    public boolean registrarRespuesta(int asistenciaId, String estado, String faltaCodigo, String faltaObservacion) throws SQLException {
        try (Connection con = getCon()) {
            boolean[] supportsRespuestaColumns = supportsColumns(con, "rasgo_asistencia", "falta_codigo", "falta_observacion", "responded_at");
            String sql = buildRespuestaUpdateSql(supportsRespuestaColumns[0], supportsRespuestaColumns[1], supportsRespuestaColumns[2]);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, estado);
                if (supportsRespuestaColumns[0]) {
                    ps.setString(index++, faltaCodigo == null || faltaCodigo.isBlank() ? null : faltaCodigo.trim().toUpperCase());
                }
                if (supportsRespuestaColumns[1]) {
                    ps.setString(index++, faltaObservacion == null ? null : faltaObservacion.trim());
                }
                ps.setInt(index, asistenciaId);
                return ps.executeUpdate() == 1;
            }
        }
    }

    public void actualizarPlanillaRasgo(int planillaId, String tema, List<UpdateRasgoAsistenciaRequest> asistencias) throws SQLException {
        try (Connection con = getCon()) {
            boolean[] supportsRespuestaColumns = supportsColumns(con, "rasgo_asistencia", "falta_codigo", "falta_observacion", "responded_at");
            String respuestaSql = buildEdicionUpdateSql(supportsRespuestaColumns[0], supportsRespuestaColumns[1], supportsRespuestaColumns[2]);
            boolean originalAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement planilla = con.prepareStatement("UPDATE planilla_rasgo SET tema = ? WHERE id = ?")) {
                    planilla.setString(1, tema);
                    planilla.setInt(2, planillaId);
                    if (planilla.executeUpdate() != 1) {
                        throw new SQLException("La planilla de rasgos no existe");
                    }
                }

                if (asistencias != null) {
                    try (PreparedStatement asistencia = con.prepareStatement(respuestaSql)) {
                        for (UpdateRasgoAsistenciaRequest request : asistencias) {
                            if (request == null || request.asistenciaId() == null || request.asistenciaId() <= 0) {
                                throw new IllegalArgumentException("El id de asistencia es requerido.");
                            }
                            String estado = normalizarEstadoEditable(request.estado());
                            if (!perteneceAPlanilla(con, request.asistenciaId(), planillaId)) {
                                throw new IllegalArgumentException("Una asistencia no pertenece a esta clase.");
                            }
                            validarCodigos(con, request.codigos());
                            int index = 1;
                            if (supportsRespuestaColumns[0]) asistencia.setString(index++, estado);
                            if (supportsRespuestaColumns[1]) asistencia.setString(index++, estado);
                            if (supportsRespuestaColumns[2]) asistencia.setString(index++, estado);
                            asistencia.setString(index++, estado);
                            asistencia.setInt(index, request.asistenciaId());
                            asistencia.addBatch();
                        }
                        asistencia.executeBatch();
                    }
                    for (UpdateRasgoAsistenciaRequest request : asistencias) {
                        reemplazarCodigos(con, request.asistenciaId(), request.codigos());
                    }
                }
                con.commit();
            } catch (SQLException | IllegalArgumentException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }

    static String normalizarEstadoEditable(String estado) {
        if ("presente".equalsIgnoreCase(estado)) return "presente";
        if ("ausente".equalsIgnoreCase(estado)) return "ausente";
        if ("ausente_justificado".equalsIgnoreCase(estado)) return "ausente_justificado";
        if ("pendiente".equalsIgnoreCase(estado)) return "pendiente";
        throw new IllegalArgumentException("El estado de asistencia no es válido.");
    }

    // Evaluate preservation before assigning estado (MySQL evaluates SET left to right).
    static String buildEdicionUpdateSql(boolean codigo, boolean observacion, boolean respondedAt) {
        StringBuilder sql = new StringBuilder("UPDATE rasgo_asistencia SET ");
        if (codigo) sql.append("falta_codigo = CASE WHEN estado = ? THEN falta_codigo ELSE NULL END, ");
        if (observacion) sql.append("falta_observacion = CASE WHEN estado = ? THEN falta_observacion ELSE NULL END, ");
        if (respondedAt) sql.append("responded_at = CASE WHEN estado = ? THEN responded_at ELSE CURRENT_TIMESTAMP END, ");
        return sql.append("estado = ? WHERE id = ?").toString();
    }

    private boolean perteneceAPlanilla(Connection con, int asistenciaId, int planillaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM rasgo_asistencia WHERE id = ? AND planilla_rasgo_id = ?")) {
            ps.setInt(1, asistenciaId);
            ps.setInt(2, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void reemplazarCodigos(Connection con, int asistenciaId, List<String> codigos) throws SQLException {
        Set<String> validos = validarCodigos(con, codigos);
        try (PreparedStatement delete = con.prepareStatement("DELETE FROM rasgo_asistencia_codigo WHERE rasgo_asistencia_id = ?")) {
            delete.setInt(1, asistenciaId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = con.prepareStatement("INSERT INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo) VALUES (?, ?)")) {
            for (String codigo : validos) {
                insert.setInt(1, asistenciaId);
                insert.setString(2, codigo);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public void reemplazarCodigos(int asistenciaId, List<String> codigos) throws SQLException {
        try (Connection con = getCon()) {
            Set<String> validos = validarCodigos(con, codigos);
            con.setAutoCommit(false);
            try (PreparedStatement delete = con.prepareStatement("DELETE FROM rasgo_asistencia_codigo WHERE rasgo_asistencia_id = ?")) {
                delete.setInt(1, asistenciaId);
                delete.executeUpdate();
                try (PreparedStatement insert = con.prepareStatement("INSERT INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo) VALUES (?, ?)")) {
                    for (String codigo : validos) {
                        insert.setInt(1, asistenciaId);
                        insert.setString(2, codigo);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    static Set<String> validarCodigos(List<String> codigos) {
        Set<String> validos = new HashSet<>();
        if (codigos == null) return validos;
        for (String codigo : codigos) {
            if (codigo == null || !codigo.trim().toUpperCase().matches("N[A-Z0-9]{0,9}")) {
                throw new IllegalArgumentException("Código de rasgo inválido: " + codigo);
            }
            validos.add(codigo.trim().toUpperCase());
        }
        return validos;
    }

    private Set<String> validarCodigos(Connection con, List<String> codigos) throws SQLException {
        Set<String> requested = validarCodigos(codigos);
        if (requested.isEmpty()) return requested;
        String placeholders = String.join(",", java.util.Collections.nCopies(requested.size(), "?"));
        Set<String> validos = new HashSet<>();
        String sql = "SELECT codigo FROM codigo_conducta WHERE activo = TRUE AND codigo IN (" + placeholders + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int index = 1;
            for (String codigo : requested) ps.setString(index++, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) validos.add(rs.getString("codigo"));
            }
        }
        if (validos.size() != requested.size()) {
            requested.removeAll(validos);
            throw new IllegalArgumentException("Código(s) de rasgo no disponible(s): " + requested);
        }
        return validos;
    }

    private List<String> listarCodigos(Connection con, int asistenciaId) throws SQLException {
        List<String> codigos = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT codigo FROM rasgo_asistencia_codigo WHERE rasgo_asistencia_id = ? ORDER BY codigo")) {
            ps.setInt(1, asistenciaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) codigos.add(rs.getString("codigo"));
            }
        }
        return codigos;
    }

    private RasgoPlanilla fromPlanillaResultSet(ResultSet rs) throws SQLException {
        RasgoPlanilla planilla = new RasgoPlanilla();
        planilla.setId(rs.getInt("id"));
        planilla.setCursoId(rs.getInt("curso_id"));
        planilla.setProfesorId(rs.getInt("profesor_id"));
        planilla.setTema(rs.getString("tema"));
        try {
            planilla.setFechaClase(rs.getDate("fecha_clase"));
        } catch (SQLException ignored) {
            planilla.setFechaClase(null);
        }
        try {
            planilla.setCreatedAt(rs.getTimestamp("created_at"));
        } catch (SQLException ignored) {
            planilla.setCreatedAt(null);
        }
        return planilla;
    }

    private RasgoAsistencia fromAsistenciaResultSet(ResultSet rs) throws SQLException {
        RasgoAsistencia asistencia = new RasgoAsistencia();
        asistencia.setId(rs.getInt("id"));
        asistencia.setPlanillaRasgoId(rs.getInt("planilla_rasgo_id"));
        asistencia.setAlumnoId(rs.getInt("alumno_id"));
        asistencia.setAlumnoNombre(rs.getString("alumno_nombre"));
        asistencia.setAlumnoApellido(rs.getString("alumno_apellido"));
        asistencia.setAlumnoEmail(rs.getString("alumno_email"));
        asistencia.setEstado(rs.getString("estado"));
        asistencia.setFaltaCodigo(rs.getString("falta_codigo"));
        asistencia.setFaltaObservacion(rs.getString("falta_observacion"));
        asistencia.setRespondedAt(rs.getTimestamp("responded_at"));
        asistencia.setTema(rs.getString("tema"));
        asistencia.setCodigoDescripcion(rs.getString("falta_codigo_descripcion"));
        return asistencia;
    }
}
