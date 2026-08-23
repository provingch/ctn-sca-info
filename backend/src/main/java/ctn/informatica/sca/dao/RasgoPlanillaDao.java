package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    private void guardarCodigosDeAlumnos(Connection con, int planillaId, List<Alumno> alumnos, Map<Integer, List<String>> codigosPorAlumno) throws SQLException {
        if (codigosPorAlumno == null || codigosPorAlumno.isEmpty()) return;
        String sql = "INSERT IGNORE INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo) "
                + "SELECT ra.id, ? FROM rasgo_asistencia ra WHERE ra.planilla_rasgo_id = ? AND ra.alumno_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Alumno alumno : alumnos) {
                List<String> codigos = codigosPorAlumno.get(alumno.getId());
                if (codigos == null) continue;
                for (String codigo : validarCodigos(codigos)) {
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

    public List<RasgoAsistencia> listarAsistencias(int planillaRasgoId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
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
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
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
                + "ra.estado, ra.falta_codigo, ra.falta_observacion, ra.responded_at, pr.tema "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
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

    public void reemplazarCodigos(int asistenciaId, List<String> codigos) throws SQLException {
        Set<String> validos = validarCodigos(codigos);
        try (Connection con = getCon()) {
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
            if (codigo == null || !codigo.trim().toUpperCase().matches("N[1-8]")) {
                throw new IllegalArgumentException("Código de rasgo inválido: " + codigo);
            }
            validos.add(codigo.trim().toUpperCase());
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
        return asistencia;
    }
}
