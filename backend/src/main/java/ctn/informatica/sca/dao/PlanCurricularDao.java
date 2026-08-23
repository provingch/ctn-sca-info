package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.TemaPlanDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.stereotype.Repository;

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

    public void rechazar(int id, int evaluadorId, String observaciones) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("UPDATE plan_curricular SET estado='RECHAZADO', evaluador_id = ?, fecha_revision = CURRENT_TIMESTAMP, observaciones_evaluador = ? WHERE id = ?")) {
            ps.setInt(1, evaluadorId); ps.setString(2, observaciones); ps.setInt(3, id); ps.executeUpdate();
        }
    }
}
