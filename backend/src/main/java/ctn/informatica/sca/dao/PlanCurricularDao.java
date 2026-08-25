package ctn.informatica.sca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.dto.TemaPlanDto;
import ctn.informatica.sca.model.Curso;

@Repository
public class PlanCurricularDao extends conexion {

    public int saveOrReplace(int asignacionId, String etapa, int anio, String nombreArchivo, byte[] contenido, List<TemaPlanDto> temas) throws SQLException {
        try (Connection c = getCon()) {
            c.setAutoCommit(false);
            try {
                // check existing
                Integer existingId = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT id, estado FROM plan_curricular WHERE asignacion_id = ? AND etapa = ? AND anio_lectivo = ?")) {
                    ps.setInt(1, asignacionId);
                    ps.setString(2, etapa);
                    ps.setInt(3, anio);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("id");
                            String estado = rs.getString("estado");
                            if ("APROBADO".equalsIgnoreCase(estado)) throw new SQLException("Ya existe un plan aprobado para esta asignación/etapa/año");
                        }
                    }
                }

                int planId = -1;
                if (existingId != null) {
                    // delete temas, update row
                    try (PreparedStatement d = c.prepareStatement("DELETE FROM tema_plan_curricular WHERE plan_curricular_id = ?")) { d.setInt(1, existingId); d.executeUpdate(); }
                    try (PreparedStatement u = c.prepareStatement("UPDATE plan_curricular SET archivo_nombre = ?, archivo_contenido = ?, estado = 'PENDIENTE', fecha_subida = CURRENT_TIMESTAMP, fecha_revision = NULL, evaluador_id = NULL, observaciones_evaluador = NULL WHERE id = ?")) {
                        u.setString(1, nombreArchivo);
                        u.setBytes(2, contenido);
                        u.setInt(3, existingId);
                        u.executeUpdate();
                    }
                    planId = existingId;
                } else {
                    try (PreparedStatement ins = c.prepareStatement("INSERT INTO plan_curricular (asignacion_id, etapa, anio_lectivo, archivo_nombre, archivo_contenido) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        ins.setInt(1, asignacionId);
                        ins.setString(2, etapa);
                        ins.setInt(3, anio);
                        ins.setString(4, nombreArchivo);
                        ins.setBytes(5, contenido);
                        ins.executeUpdate();
                        try (ResultSet keys = ins.getGeneratedKeys()) { if (keys.next()) planId = keys.getInt(1); }
                    }
                }

                // insert temas
                if (temas != null && planId != -1) {
                    try (PreparedStatement tps = c.prepareStatement("INSERT INTO tema_plan_curricular (plan_curricular_id, mes, orden_mes, bloque, capacidades, temas_contenidos, actividades, instrumentos_evaluacion, indicador_conceptual, indicador_procedimental, indicador_actitudinal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        for (TemaPlanDto t : temas) {
                            tps.setInt(1, planId);
                            tps.setString(2, t.mes);
                            tps.setInt(3, t.ordenMes);
                            tps.setInt(4, t.bloque);
                            tps.setString(5, t.capacidades);
                            tps.setString(6, t.temasContenidos);
                            tps.setString(7, t.actividades);
                            tps.setString(8, t.instrumentos);
                            tps.setString(9, t.indicadorConceptual);
                            tps.setString(10, t.indicadorProcedimental);
                            tps.setString(11, t.indicadorActitudinal);
                            tps.addBatch();
                        }
                        tps.executeBatch();
                    }
                }

                c.commit();
                return planId;
            } catch (SQLException ex) { c.rollback(); throw ex; } finally { c.setAutoCommit(true); }
        }
    }

    public void aprobar(int id, int evaluadorId) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("UPDATE plan_curricular SET estado='APROBADO', evaluador_id = ?, fecha_revision = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, evaluadorId); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public Integer findProfesorIdByPlanId(int planId) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT a.usuario_id AS profesor_id FROM plan_curricular p JOIN asignacion a ON a.id = p.asignacion_id WHERE p.id = ?")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("profesor_id");
                }
            }
        }
        return null;
    }

    public void rechazar(int id, int evaluadorId, String observaciones) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("UPDATE plan_curricular SET estado='RECHAZADO', evaluador_id = ?, fecha_revision = CURRENT_TIMESTAMP, observaciones_evaluador = ? WHERE id = ?")) {
            ps.setInt(1, evaluadorId); ps.setString(2, observaciones); ps.setInt(3, id); ps.executeUpdate();
        }
    }

    public PlanCurricularDto findByAsignacion(int asignacionId, String etapa, int anio) throws SQLException {
        String sql = "SELECT p.id, p.estado, p.archivo_nombre, p.fecha_subida, p.fecha_revision, p.observaciones_evaluador, " +
                "t.id AS tema_id, t.mes, t.orden_mes, t.bloque, t.capacidades, t.temas_contenidos, t.actividades, " +
                "t.instrumentos_evaluacion, t.indicador_conceptual, t.indicador_procedimental, t.indicador_actitudinal " +
                "FROM plan_curricular p " +
                "LEFT JOIN tema_plan_curricular t ON t.plan_curricular_id = p.id " +
                "WHERE p.asignacion_id = ? AND p.etapa = ? AND p.anio_lectivo = ? " +
                "ORDER BY t.orden_mes ASC";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setString(2, etapa);
            ps.setInt(3, anio);
            try (ResultSet rs = ps.executeQuery()) {
                PlanCurricularDto dto = null;
                List<TemaPlanDto> temas = new ArrayList<>();
                while (rs.next()) {
                    if (dto == null) {
                        dto = new PlanCurricularDto();
                        dto.id = rs.getInt("id");
                        dto.estado = rs.getString("estado");
                        dto.archivoNombre = rs.getString("archivo_nombre");
                        dto.fechaSubida = rs.getString("fecha_subida");
                        dto.fechaRevision = rs.getString("fecha_revision");
                        dto.observacionesEvaluador = rs.getString("observaciones_evaluador");
                        dto.etapa = etapa;
                        dto.anio = anio;
                    }
                    Integer temaId = rs.getObject("tema_id", Integer.class);
                    if (temaId != null) {
                        TemaPlanDto t = new TemaPlanDto();
                        t.mes = rs.getString("mes");
                        t.ordenMes = rs.getInt("orden_mes");
                        t.bloque = rs.getInt("bloque");
                        t.capacidades = rs.getString("capacidades");
                        t.temasContenidos = rs.getString("temas_contenidos");
                        t.actividades = rs.getString("actividades");
                        t.instrumentos = rs.getString("instrumentos_evaluacion");
                        t.indicadorConceptual = rs.getString("indicador_conceptual");
                        t.indicadorProcedimental = rs.getString("indicador_procedimental");
                        t.indicadorActitudinal = rs.getString("indicador_actitudinal");
                        temas.add(t);
                    }
                }
                if (dto != null) {
                    dto.temas = temas;
                }
                return dto;
            }
        }
    }

    public List<PlanCurricularDto> findPendientes() throws SQLException {
        String sql = "SELECT p.id, p.estado, p.archivo_nombre, p.fecha_subida, p.fecha_revision, p.observaciones_evaluador, " +
                "a.id AS asignacion_id, m.nombre AS materia_nombre, u.apellido AS profesor_apellido, u.nombre AS profesor_nombre, " +
                "c.promocion, c.seccion, e.nombre AS especialidad " +
                "FROM plan_curricular p " +
                "JOIN asignacion a ON a.id = p.asignacion_id " +
                "JOIN materia m ON m.id = a.materia_id " +
                "JOIN usuario u ON u.id = a.usuario_id " +
                "JOIN curso c ON c.id = a.curso_id " +
                "JOIN especialidad e ON e.id = c.especialidad_id " +
                "WHERE p.estado = 'PENDIENTE' " +
                "ORDER BY p.fecha_subida ASC";
        List<PlanCurricularDto> result = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlanCurricularDto dto = new PlanCurricularDto();
                dto.id = rs.getInt("id");
                dto.estado = rs.getString("estado");
                dto.archivoNombre = rs.getString("archivo_nombre");
                dto.fechaSubida = rs.getString("fecha_subida");
                dto.materiaNombre = rs.getString("materia_nombre");
                String profApellido = rs.getString("profesor_apellido");
                String profNombre = rs.getString("profesor_nombre");
                String profNombreCompleto = (profApellido == null ? "" : profApellido) + 
                        (profNombre == null ? "" : (profNombre.isBlank() ? "" : (" " + profNombre)));
                dto.profesorNombre = profNombreCompleto;
                String especialidad = rs.getString("especialidad");
                String seccion = rs.getString("seccion");
                String cursoDesc = (especialidad == null ? "" : especialidad) + 
                        (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                dto.cursoDescripcion = cursoDesc;
                dto.especialidad = especialidad;
                result.add(dto);
            }
        }
        return result;
    }

    public PlanCurricularDto findById(int id) throws SQLException {
        String sql = "SELECT p.id, p.estado, p.archivo_nombre, p.fecha_subida, p.fecha_revision, p.observaciones_evaluador, " +
                "p.etapa, p.anio_lectivo, a.id AS asignacion_id, " +
                "m.nombre AS materia_nombre, u.apellido AS profesor_apellido, u.nombre AS profesor_nombre, " +
                "c.promocion, c.seccion, e.nombre AS especialidad, " +
                "t.id AS tema_id, t.mes, t.orden_mes, t.bloque, t.capacidades, t.temas_contenidos, t.actividades, " +
                "t.instrumentos_evaluacion, t.indicador_conceptual, t.indicador_procedimental, t.indicador_actitudinal " +
                "FROM plan_curricular p " +
                "JOIN asignacion a ON a.id = p.asignacion_id " +
                "JOIN materia m ON m.id = a.materia_id " +
                "JOIN usuario u ON u.id = a.usuario_id " +
                "JOIN curso c ON c.id = a.curso_id " +
                "JOIN especialidad e ON e.id = c.especialidad_id " +
                "LEFT JOIN tema_plan_curricular t ON t.plan_curricular_id = p.id " +
                "WHERE p.id = ? " +
                "ORDER BY t.orden_mes ASC";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                PlanCurricularDto dto = null;
                List<TemaPlanDto> temas = new ArrayList<>();
                while (rs.next()) {
                    if (dto == null) {
                        dto = new PlanCurricularDto();
                        dto.id = rs.getInt("id");
                        dto.estado = rs.getString("estado");
                        dto.archivoNombre = rs.getString("archivo_nombre");
                        dto.fechaSubida = rs.getString("fecha_subida");
                        dto.fechaRevision = rs.getString("fecha_revision");
                        dto.observacionesEvaluador = rs.getString("observaciones_evaluador");
                        dto.etapa = rs.getString("etapa");
                        dto.anio = rs.getInt("anio_lectivo");
                        String profApellido = rs.getString("profesor_apellido");
                        String profNombre = rs.getString("profesor_nombre");
                        dto.profesorNombre = (profApellido == null ? "" : profApellido) +
                            (profNombre == null ? "" : (profNombre.isBlank() ? "" : (" " + profNombre)));
                        String especialidad = rs.getString("especialidad");
                        String seccion = rs.getString("seccion");
                        dto.especialidad = especialidad;
                        dto.cursoDescripcion = (especialidad == null ? "" : especialidad) +
                            (seccion == null || seccion.isBlank() ? "" : (" " + seccion));
                        dto.materiaNombre = rs.getString("materia_nombre");
                    }
                    Integer temaId = rs.getObject("tema_id", Integer.class);
                    if (temaId != null) {
                        TemaPlanDto t = new TemaPlanDto();
                        t.mes = rs.getString("mes");
                        t.ordenMes = rs.getInt("orden_mes");
                        t.bloque = rs.getInt("bloque");
                        t.capacidades = rs.getString("capacidades");
                        t.temasContenidos = rs.getString("temas_contenidos");
                        t.actividades = rs.getString("actividades");
                        t.instrumentos = rs.getString("instrumentos_evaluacion");
                        t.indicadorConceptual = rs.getString("indicador_conceptual");
                        t.indicadorProcedimental = rs.getString("indicador_procedimental");
                        t.indicadorActitudinal = rs.getString("indicador_actitudinal");
                        temas.add(t);
                    }
                }
                if (dto != null) {
                    dto.temas = temas;
                }
                return dto;
            }
        }
    }

    public byte[] getArchivoOriginal(int id) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT archivo_contenido FROM plan_curricular WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("archivo_contenido");
                }
                return null;
            }
        }
    }

    public List<PlanCurricularDto> findAllByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT p.id, p.estado, p.archivo_nombre, p.fecha_subida, p.fecha_revision, p.observaciones_evaluador, "
                + "p.etapa, p.anio_lectivo, a.id AS asignacion_id, c.id AS curso_id, m.nombre AS materia_nombre, "
                + "e.id AS especialidad_id, e.nombre AS especialidad, c.promocion, c.seccion "
                + "FROM plan_curricular p "
                + "JOIN asignacion a ON a.id = p.asignacion_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = a.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE a.usuario_id = ? ORDER BY p.fecha_subida DESC";
        List<PlanCurricularDto> result = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlanCurricularDto dto = new PlanCurricularDto();
                    dto.id = rs.getInt("id");
                    dto.estado = rs.getString("estado");
                    dto.archivoNombre = rs.getString("archivo_nombre");
                    dto.fechaSubida = rs.getString("fecha_subida");
                    dto.fechaRevision = rs.getString("fecha_revision");
                    dto.observacionesEvaluador = rs.getString("observaciones_evaluador");
                    dto.etapa = rs.getString("etapa");
                    dto.anio = rs.getInt("anio_lectivo");
                    dto.asignacionId = rs.getInt("asignacion_id");
                    dto.materiaNombre = rs.getString("materia_nombre");
                    dto.especialidadId = rs.getInt("especialidad_id");
                    dto.especialidadNombre = rs.getString("especialidad");
                    dto.seccion = rs.getString("seccion");
                    dto.cursoOrdinal = new Curso(rs.getInt("curso_id"), dto.especialidadNombre,
                            rs.getInt("promocion"), dto.seccion).getCursoOrdinal();
                    result.add(dto);
                }
            }
        }
        return result;
    }

    public boolean existeAprobado(int asignacionId, String etapa, int anio) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM plan_curricular WHERE asignacion_id = ? AND etapa = ? AND anio_lectivo = ? AND estado = 'APROBADO'")) {
            ps.setInt(1, asignacionId);
            ps.setString(2, etapa);
            ps.setInt(3, anio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void marcarCubierto(int temaId, int planillaRasgoId) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("UPDATE tema_plan_curricular SET estado_cobertura = 'CUBIERTO', fecha_cobertura = CURRENT_TIMESTAMP, planilla_rasgo_id = ? WHERE id = ?")) {
            ps.setInt(1, planillaRasgoId);
            ps.setInt(2, temaId);
            ps.executeUpdate();
        }
    }
}
