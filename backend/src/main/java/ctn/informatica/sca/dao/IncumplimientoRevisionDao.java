package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class IncumplimientoRevisionDao extends conexion {

    /** Clase dada sin plan aprobado que, al aprobarse el plan, no coincide con lo planificado. */
    public static final String TIPO_INCONGRUENCIA_RETROACTIVA = "INCONGRUENCIA_RETROACTIVA";
    /** Bloqueo de Iniciar clase por acumular rechazos de incongruencias retroactivas; sin vencimiento. */
    public static final String TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA = "BLOQUEO_INCONGRUENCIA_RETROACTIVA";

    public int registrar(int asignacionId, int usuarioId, Integer temaPlanCurricularId, String tipo, String descripcion,
            String estado, Integer evaluadorId, LocalDateTime suspensionDesde, LocalDateTime suspensionHasta) throws SQLException {
        String sql = "INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tema_plan_curricular_id, tipo, descripcion, estado, evaluador_id, fecha_resolucion, suspension_desde, suspension_hasta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            if (temaPlanCurricularId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, temaPlanCurricularId);
            }
            ps.setString(4, tipo == null || tipo.isBlank() ? "ATRASO" : tipo.trim().toUpperCase());
            ps.setString(5, descripcion == null ? "" : descripcion.trim());
            ps.setString(6, estado == null || estado.isBlank() ? "PENDIENTE" : estado.trim().toUpperCase());
            if (evaluadorId == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, evaluadorId);
            }
            if (estado != null && "PENDIENTE".equalsIgnoreCase(estado)) {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            }
            if (suspensionDesde == null) {
                ps.setNull(9, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(suspensionDesde));
            }
            if (suspensionHasta == null) {
                ps.setNull(10, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(10, Timestamp.valueOf(suspensionHasta));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    /** Inserta una incongruencia retroactiva PENDIENTE ligada a la clase (planilla_rasgo) que la origina. */
    public int registrarIncongruenciaRetroactiva(int asignacionId, int usuarioId, Integer temaPlanCurricularId,
            int planillaRasgoId, String descripcion) throws SQLException {
        String sql = "INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tema_plan_curricular_id, planilla_rasgo_id, tipo, descripcion, estado) "
                + "VALUES (?, ?, ?, ?, '" + TIPO_INCONGRUENCIA_RETROACTIVA + "', ?, 'PENDIENTE')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            if (temaPlanCurricularId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, temaPlanCurricularId);
            }
            ps.setInt(4, planillaRasgoId);
            ps.setString(5, descripcion == null ? "" : descripcion.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    /** Inserta el bloqueo PENDIENTE de Iniciar clase por acumulación de rechazos retroactivos. */
    public int registrarBloqueoIncongruenciaRetroactiva(int asignacionId, int usuarioId, String descripcion) throws SQLException {
        String sql = "INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tipo, descripcion, estado) "
                + "VALUES (?, ?, '" + TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA + "', ?, 'PENDIENTE')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setString(3, descripcion == null ? "" : descripcion.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean existeIncongruenciaRetroactivaPorClase(int planillaRasgoId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM incumplimiento_revision WHERE planilla_rasgo_id = ? AND tipo = '" + TIPO_INCONGRUENCIA_RETROACTIVA + "')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaRasgoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    /**
     * Rechazos de incongruencias retroactivas del profesor en la asignación desde el último bloqueo
     * levantado: al reactivar, la tolerancia arranca de cero (si no, cualquier rechazo posterior
     * volvería a bloquear al instante porque el acumulado ya superó el umbral).
     */
    public long contarRechazadosRetroactivos(int asignacionId, int usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM incumplimiento_revision WHERE tipo = '" + TIPO_INCONGRUENCIA_RETROACTIVA + "' AND estado = 'RECHAZADO' "
                + "AND asignacion_id = ? AND usuario_id = ? "
                + "AND fecha_resolucion > COALESCE((SELECT MAX(b.fecha_resolucion) FROM incumplimiento_revision b "
                + "WHERE b.tipo = '" + TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA + "' AND b.estado <> 'PENDIENTE' AND b.asignacion_id = ? AND b.usuario_id = ?), '1970-01-01')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setInt(3, asignacionId);
            ps.setInt(4, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    /** Incongruencias retroactivas pendientes de revisión del profesor, con el detalle de la clase. */
    public List<Map<String, Object>> listarIncongruenciasPendientesPorUsuario(int usuarioId) throws SQLException {
        return listarConDetalle("WHERE ir.estado = 'PENDIENTE' AND ir.tipo = '" + TIPO_INCONGRUENCIA_RETROACTIVA + "' AND ir.usuario_id = ? ", usuarioId);
    }

    /** Guarda la justificación del profesor mientras la incongruencia siga PENDIENTE y sea suya. */
    public boolean justificar(int id, int usuarioId, String justificacion) throws SQLException {
        String sql = "UPDATE incumplimiento_revision SET justificacion_profesor = ? "
                + "WHERE id = ? AND usuario_id = ? AND tipo = '" + TIPO_INCONGRUENCIA_RETROACTIVA + "' AND estado = 'PENDIENTE'";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, justificacion == null ? null : justificacion.trim());
            ps.setInt(2, id);
            ps.setInt(3, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }

    public long contarPorAsignacionYUsuario(int asignacionId, int usuarioId, String tipo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM incumplimiento_revision WHERE asignacion_id = ? AND usuario_id = ? AND tipo = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setString(3, tipo == null || tipo.isBlank() ? "ATRASO" : tipo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    public boolean existePendientePorAsignacionYUsuario(int asignacionId, int usuarioId, String tipo) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM incumplimiento_revision "
                + "WHERE asignacion_id = ? AND usuario_id = ? AND tipo = ? AND estado = 'PENDIENTE')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setString(3, tipo == null || tipo.isBlank() ? "ATRASO" : tipo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    public List<Map<String, Object>> listarPendientes() throws SQLException {
        return listarConDetalle("WHERE ir.estado = 'PENDIENTE' ", null);
    }

    /**
     * Para las filas ligadas a una clase (planilla_rasgo) trae el detalle: fecha, tema ingresado y tema
     * esperado según el plan. Los otros tipos devuelven esos campos en null.
     */
    private List<Map<String, Object>> listarConDetalle(String where, Integer usuarioId) throws SQLException {
        String sql = "SELECT ir.id, ir.asignacion_id, ir.usuario_id, ir.tipo, ir.descripcion, ir.estado, ir.created_at, ir.evaluador_id, ir.fecha_resolucion, ir.suspension_desde, ir.suspension_hasta, "
                + "ir.planilla_rasgo_id, ir.justificacion_profesor, "
                + "u.nombre AS usuario_nombre, u.apellido AS usuario_apellido, a.materia_id, a.curso_base_id, "
                + "m.nombre AS materia_nombre, pr.fecha_clase, pr.tema AS tema_ingresado, t.temas_contenidos AS tema_esperado "
                + "FROM incumplimiento_revision ir "
                + "LEFT JOIN usuario u ON u.id = ir.usuario_id "
                + "LEFT JOIN asignacion a ON a.id = ir.asignacion_id "
                + "LEFT JOIN materia m ON m.id = a.materia_id "
                + "LEFT JOIN planilla_rasgo pr ON pr.id = ir.planilla_rasgo_id "
                + "LEFT JOIN tema_plan_curricular t ON t.id = ir.tema_plan_curricular_id "
                + where + "ORDER BY ir.created_at DESC, ir.id DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (usuarioId != null) {
                ps.setInt(1, usuarioId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("asignacionId", rs.getInt("asignacion_id"));
                    row.put("usuarioId", rs.getInt("usuario_id"));
                    row.put("tipo", rs.getString("tipo"));
                    row.put("descripcion", rs.getString("descripcion"));
                    row.put("estado", rs.getString("estado"));
                    row.put("fechaCreacion", rs.getTimestamp("created_at"));
                    row.put("evaluadorId", rs.getObject("evaluador_id", Integer.class));
                    row.put("fechaResolucion", rs.getTimestamp("fecha_resolucion"));
                    row.put("suspensionDesde", rs.getTimestamp("suspension_desde"));
                    row.put("suspensionHasta", rs.getTimestamp("suspension_hasta"));
                    row.put("usuarioNombre", rs.getString("usuario_nombre"));
                    row.put("usuarioApellido", rs.getString("usuario_apellido"));
                    row.put("materiaId", rs.getObject("materia_id", Integer.class));
                    row.put("cursoId", rs.getObject("curso_base_id", Integer.class));
                    row.put("planillaRasgoId", rs.getObject("planilla_rasgo_id", Integer.class));
                    row.put("justificacionProfesor", rs.getString("justificacion_profesor"));
                    row.put("materiaNombre", rs.getString("materia_nombre"));
                    java.sql.Date fechaClase = rs.getDate("fecha_clase");
                    row.put("fechaClase", fechaClase == null ? null : fechaClase.toLocalDate());
                    row.put("temaIngresado", rs.getString("tema_ingresado"));
                    row.put("temaEsperado", rs.getString("tema_esperado"));
                    items.add(row);
                }
            }
        }
        return items;
    }

    public Map<String, Object> findById(int id) throws SQLException {
        String sql = "SELECT id, asignacion_id, usuario_id, tipo, estado, evaluador_id, suspension_desde, suspension_hasta, planilla_rasgo_id, justificacion_profesor "
                + "FROM incumplimiento_revision WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("asignacionId", rs.getInt("asignacion_id"));
                    row.put("usuarioId", rs.getInt("usuario_id"));
                    row.put("tipo", rs.getString("tipo"));
                    row.put("estado", rs.getString("estado"));
                    row.put("evaluadorId", rs.getObject("evaluador_id", Integer.class));
                    row.put("suspensionDesde", rs.getTimestamp("suspension_desde"));
                    row.put("suspensionHasta", rs.getTimestamp("suspension_hasta"));
                    row.put("planillaRasgoId", rs.getObject("planilla_rasgo_id", Integer.class));
                    row.put("justificacionProfesor", rs.getString("justificacion_profesor"));
                    return row;
                }
            }
        }
        return null;
    }

    /**
     * Una incongruencia retroactiva pendiente NO bloquea por sí sola (recién bloquea al llegar al umbral
     * de rechazos, vía la fila BLOQUEO_INCONGRUENCIA_RETROACTIVA, que dura hasta que evaluación la resuelva).
     * El resto de los tipos conserva el comportamiento de siempre: pendiente, o rechazado dentro de su suspensión.
     */
    public boolean existeBloqueoActivo(int asignacionId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM incumplimiento_revision WHERE asignacion_id = ? AND estado = 'PENDIENTE' AND tipo NOT IN ('" + TIPO_INCONGRUENCIA_RETROACTIVA + "','" + TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA + "')) "
                + "OR EXISTS(SELECT 1 FROM incumplimiento_revision WHERE asignacion_id = ? AND estado = 'RECHAZADO' AND suspension_desde <= NOW() AND suspension_hasta >= NOW()) "
                + "OR EXISTS(SELECT 1 FROM incumplimiento_revision WHERE asignacion_id = ? AND tipo = '" + TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA + "' AND estado = 'PENDIENTE')";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, asignacionId);
            ps.setInt(3, asignacionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    public boolean resolver(int id, String estado, int evaluadorId) throws SQLException {
        return resolver(id, estado, evaluadorId, null, null);
    }

    public boolean resolver(int id, String estado, int evaluadorId, java.time.LocalDateTime suspensionDesde, java.time.LocalDateTime suspensionHasta) throws SQLException {
        return resolver(id, estado, evaluadorId, suspensionDesde, suspensionHasta, null);
    }

    /** {@code nota} es la nota libre de evaluación al levantar un bloqueo retroactivo (null en el resto). */
    public boolean resolver(int id, String estado, int evaluadorId, java.time.LocalDateTime suspensionDesde, java.time.LocalDateTime suspensionHasta, String nota) throws SQLException {
        String sql = "UPDATE incumplimiento_revision SET estado = ?, evaluador_id = ?, fecha_resolucion = CURRENT_TIMESTAMP, suspension_desde = ?, suspension_hasta = ?, nota_resolucion = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            String normalizedEstado = estado == null || estado.isBlank() ? "PERMITIDO" : estado.trim().toUpperCase();
            ps.setString(1, normalizedEstado);
            ps.setInt(2, evaluadorId);
            if (suspensionDesde == null) {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(3, Timestamp.valueOf(suspensionDesde));
            }
            if (suspensionHasta == null) {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(suspensionHasta));
            }
            if (nota == null || nota.isBlank()) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, nota.trim());
            }
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        }
    }
}
