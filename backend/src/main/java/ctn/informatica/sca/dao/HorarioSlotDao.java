package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.HorarioSlot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class HorarioSlotDao extends conexion {

    private HorarioSlot fromResultSet(ResultSet rs) throws SQLException {
        HorarioSlot slot = new HorarioSlot();
        slot.setId(rs.getInt("id"));
        slot.setAsignacionId(rs.getInt("asignacion_id"));
        slot.setUsuarioId(rs.getInt("usuario_id"));
        slot.setCursoId(rs.getInt("curso_id"));
        slot.setDiaSemana(rs.getInt("dia_semana"));
        slot.setHoraCatedraId(rs.getInt("hora_catedra_id"));
        slot.setSala(rs.getString("sala"));

        slot.setMateriaNombre(rs.getString("materia_nombre"));
        slot.setCursoDescripcion(rs.getString("curso_descripcion"));
        slot.setProfesorNombre(rs.getString("profesor_nombre"));
        slot.setHoraCatedraNumero(rs.getObject("hora_catedra_numero") == null ? null : rs.getInt("hora_catedra_numero"));
        slot.setHoraCatedraEtiqueta(rs.getString("hora_catedra_etiqueta"));
        slot.setHoraInicio(rs.getString("hora_inicio"));
        slot.setHoraFin(rs.getString("hora_fin"));
        return slot;
    }

    private String buildFullName(String apellido, String nombre) {
        StringBuilder sb = new StringBuilder();
        if (apellido != null && !apellido.isBlank()) {
            sb.append(apellido.trim());
        }
        if (nombre != null && !nombre.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(nombre.trim());
        }
        return sb.toString();
    }

    public List<HorarioSlot> findByAsignacion(int asignacionId) throws SQLException {
        String sql = "SELECT hs.id, hs.asignacion_id, hs.usuario_id, hs.curso_id, hs.dia_semana, hs.hora_catedra_id, hs.sala, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "hc.numero AS hora_catedra_numero, hc.etiqueta AS hora_catedra_etiqueta, "
                + "TIME_FORMAT(hc.hora_inicio, '%H:%i') AS hora_inicio, TIME_FORMAT(hc.hora_fin, '%H:%i') AS hora_fin "
                + "FROM horario_slot hs "
                + "JOIN asignacion a ON a.id = hs.asignacion_id "
                + "JOIN usuario u ON u.id = hs.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = hs.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "JOIN hora_catedra hc ON hc.id = hs.hora_catedra_id "
                + "WHERE hs.asignacion_id = ? "
                + "ORDER BY hs.dia_semana, hc.numero";
        List<HorarioSlot> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HorarioSlot slot = new HorarioSlot();
                    slot.setId(rs.getInt("id"));
                    slot.setAsignacionId(rs.getInt("asignacion_id"));
                    slot.setUsuarioId(rs.getInt("usuario_id"));
                    slot.setCursoId(rs.getInt("curso_id"));
                    slot.setDiaSemana(rs.getInt("dia_semana"));
                    slot.setHoraCatedraId(rs.getInt("hora_catedra_id"));
                    slot.setSala(rs.getString("sala"));
                    slot.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    String seccion = rs.getString("seccion");
                    slot.setCursoDescripcion((especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion)));
                    slot.setProfesorNombre(buildFullName(rs.getString("profesor_apellido"), rs.getString("profesor_nombre")));
                    slot.setHoraCatedraNumero(rs.getInt("hora_catedra_numero"));
                    slot.setHoraCatedraEtiqueta(rs.getString("hora_catedra_etiqueta"));
                    slot.setHoraInicio(rs.getString("hora_inicio"));
                    slot.setHoraFin(rs.getString("hora_fin"));
                    out.add(slot);
                }
            }
        }
        return out;
    }

    public List<HorarioSlot> findByProfesor(int usuarioId) throws SQLException {
        String sql = "SELECT hs.id, hs.asignacion_id, hs.usuario_id, hs.curso_id, hs.dia_semana, hs.hora_catedra_id, hs.sala, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "hc.numero AS hora_catedra_numero, hc.etiqueta AS hora_catedra_etiqueta, "
                + "TIME_FORMAT(hc.hora_inicio, '%H:%i') AS hora_inicio, TIME_FORMAT(hc.hora_fin, '%H:%i') AS hora_fin "
                + "FROM horario_slot hs "
                + "JOIN asignacion a ON a.id = hs.asignacion_id "
                + "JOIN usuario u ON u.id = hs.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = hs.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "JOIN hora_catedra hc ON hc.id = hs.hora_catedra_id "
                + "WHERE hs.usuario_id = ? "
                + "ORDER BY hs.dia_semana, hc.numero";
        List<HorarioSlot> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HorarioSlot slot = new HorarioSlot();
                    slot.setId(rs.getInt("id"));
                    slot.setAsignacionId(rs.getInt("asignacion_id"));
                    slot.setUsuarioId(rs.getInt("usuario_id"));
                    slot.setCursoId(rs.getInt("curso_id"));
                    slot.setDiaSemana(rs.getInt("dia_semana"));
                    slot.setHoraCatedraId(rs.getInt("hora_catedra_id"));
                    slot.setSala(rs.getString("sala"));
                    slot.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    String seccion = rs.getString("seccion");
                    slot.setCursoDescripcion((especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion)));
                    slot.setProfesorNombre(buildFullName(rs.getString("profesor_apellido"), rs.getString("profesor_nombre")));
                    slot.setHoraCatedraNumero(rs.getInt("hora_catedra_numero"));
                    slot.setHoraCatedraEtiqueta(rs.getString("hora_catedra_etiqueta"));
                    slot.setHoraInicio(rs.getString("hora_inicio"));
                    slot.setHoraFin(rs.getString("hora_fin"));
                    out.add(slot);
                }
            }
        }
        return out;
    }

    public List<ctn.informatica.sca.dto.HorarioResumenCursoDto> resumenPorCurso() throws SQLException {
        String sql = "SELECT c.id AS curso_id, e.nombre AS especialidad, c.promocion AS promocion, c.seccion AS seccion, "
                + "COUNT(hs.id) AS cantidad_slots_cargados "
                + "FROM curso c "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "LEFT JOIN asignacion a ON a.curso_id = c.id "
                + "LEFT JOIN horario_slot hs ON hs.asignacion_id = a.id "
                + "GROUP BY c.id, e.nombre, c.promocion, c.seccion "
                + "ORDER BY e.nombre, c.promocion DESC, c.seccion, c.id";
        List<ctn.informatica.sca.dto.HorarioResumenCursoDto> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int period = ctn.informatica.sca.util.AcademicPeriod.current();
            while (rs.next()) {
                int cursoId = rs.getInt("curso_id");
                String especialidad = rs.getString("especialidad");
                int promocion = rs.getInt("promocion");
                String seccion = rs.getString("seccion");
                int cantidad = rs.getInt("cantidad_slots_cargados");

                int nivel = period - promocion + 3;
                if (nivel < 1) nivel = 1;
                if (nivel > 3) nivel = 3;

                String cursoDescripcion = nivel + "°" + (seccion == null || seccion.isBlank() ? "" : (" " + seccion));

                out.add(new ctn.informatica.sca.dto.HorarioResumenCursoDto(
                        cursoId,
                        especialidad,
                        cursoDescripcion,
                        cantidad,
                        nivel));
            }
        }
        return out;
    }

    public List<HorarioSlot> findByCurso(int cursoId) throws SQLException {
        String sql = "SELECT hs.id, hs.asignacion_id, hs.usuario_id, hs.curso_id, hs.dia_semana, hs.hora_catedra_id, hs.sala, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.promocion, c.seccion, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "hc.numero AS hora_catedra_numero, hc.etiqueta AS hora_catedra_etiqueta, "
                + "TIME_FORMAT(hc.hora_inicio, '%H:%i') AS hora_inicio, TIME_FORMAT(hc.hora_fin, '%H:%i') AS hora_fin "
                + "FROM horario_slot hs "
                + "JOIN asignacion a ON a.id = hs.asignacion_id "
                + "JOIN usuario u ON u.id = hs.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id "
                + "JOIN curso c ON c.id = hs.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "JOIN hora_catedra hc ON hc.id = hs.hora_catedra_id "
                + "WHERE hs.curso_id = ? "
                + "ORDER BY hs.dia_semana, hc.numero";
        List<HorarioSlot> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HorarioSlot slot = new HorarioSlot();
                    slot.setId(rs.getInt("id"));
                    slot.setAsignacionId(rs.getInt("asignacion_id"));
                    slot.setUsuarioId(rs.getInt("usuario_id"));
                    slot.setCursoId(rs.getInt("curso_id"));
                    slot.setDiaSemana(rs.getInt("dia_semana"));
                    slot.setHoraCatedraId(rs.getInt("hora_catedra_id"));
                    slot.setSala(rs.getString("sala"));
                    slot.setMateriaNombre(rs.getString("materia_nombre"));
                    String especialidad = rs.getString("especialidad");
                    String seccion = rs.getString("seccion");
                    slot.setCursoDescripcion((especialidad == null ? "" : especialidad) + (seccion == null || seccion.isBlank() ? "" : (" " + seccion)));
                    slot.setProfesorNombre(buildFullName(rs.getString("profesor_apellido"), rs.getString("profesor_nombre")));
                    slot.setHoraCatedraNumero(rs.getInt("hora_catedra_numero"));
                    slot.setHoraCatedraEtiqueta(rs.getString("hora_catedra_etiqueta"));
                    slot.setHoraInicio(rs.getString("hora_inicio"));
                    slot.setHoraFin(rs.getString("hora_fin"));
                    out.add(slot);
                }
            }
        }
        return out;
    }

    public int crear(int asignacionId, int usuarioId, int cursoId, int diaSemana, int horaCatedraId, String sala) throws SQLException {
        String sql = "INSERT INTO horario_slot (asignacion_id, usuario_id, curso_id, dia_semana, hora_catedra_id, sala) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setInt(3, cursoId);
            ps.setInt(4, diaSemana);
            ps.setInt(5, horaCatedraId);
            ps.setString(6, sala);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM horario_slot WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int eliminarPorCurso(int cursoId) throws SQLException {
        String sql = "DELETE FROM horario_slot WHERE curso_id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            return ps.executeUpdate();
        }
    }

    public HorarioSlot findProfesorConflictDetail(int usuarioId, int diaSemana, int horaCatedraId) throws SQLException {
        String sql = "SELECT hs.id, hs.asignacion_id, hs.usuario_id, hs.curso_id, hs.dia_semana, hs.hora_catedra_id, hs.sala, "
                + "m.nombre AS materia_nombre, e.nombre AS especialidad, c.seccion, u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "hc.numero AS hora_catedra_numero, hc.etiqueta AS hora_catedra_etiqueta, TIME_FORMAT(hc.hora_inicio, '%H:%i') AS hora_inicio, TIME_FORMAT(hc.hora_fin, '%H:%i') AS hora_fin "
                + "FROM horario_slot hs JOIN asignacion a ON a.id = hs.asignacion_id JOIN usuario u ON u.id = hs.usuario_id "
                + "JOIN materia m ON m.id = a.materia_id JOIN curso c ON c.id = hs.curso_id JOIN especialidad e ON e.id = c.especialidad_id "
                + "JOIN hora_catedra hc ON hc.id = hs.hora_catedra_id WHERE hs.usuario_id = ? AND hs.dia_semana = ? AND hs.hora_catedra_id = ? LIMIT 1";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, usuarioId); ps.setInt(2, diaSemana); ps.setInt(3, horaCatedraId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                HorarioSlot slot = new HorarioSlot();
                slot.setId(rs.getInt("id")); slot.setAsignacionId(rs.getInt("asignacion_id"));
                slot.setUsuarioId(rs.getInt("usuario_id")); slot.setCursoId(rs.getInt("curso_id"));
                slot.setDiaSemana(rs.getInt("dia_semana")); slot.setHoraCatedraId(rs.getInt("hora_catedra_id"));
                slot.setSala(rs.getString("sala")); slot.setMateriaNombre(rs.getString("materia_nombre"));
                String specialty = rs.getString("especialidad"); String section = rs.getString("seccion");
                slot.setCursoDescripcion((specialty == null ? "" : specialty) + (section == null || section.isBlank() ? "" : " " + section));
                slot.setProfesorNombre(buildFullName(rs.getString("profesor_apellido"), rs.getString("profesor_nombre")));
                slot.setHoraCatedraNumero(rs.getInt("hora_catedra_numero")); slot.setHoraCatedraEtiqueta(rs.getString("hora_catedra_etiqueta"));
                slot.setHoraInicio(rs.getString("hora_inicio")); slot.setHoraFin(rs.getString("hora_fin"));
                return slot;
            }
        }
    }

    public boolean existeProfesorConflict(int usuarioId, int diaSemana, int horaCatedraId) throws SQLException {
        String sql = "SELECT 1 FROM horario_slot WHERE usuario_id = ? AND dia_semana = ? AND hora_catedra_id = ? LIMIT 1";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setInt(2, diaSemana);
            ps.setInt(3, horaCatedraId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existeCursoConflict(int cursoId, int diaSemana, int horaCatedraId) throws SQLException {
        String sql = "SELECT 1 FROM horario_slot WHERE curso_id = ? AND dia_semana = ? AND hora_catedra_id = ? LIMIT 1";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            ps.setInt(2, diaSemana);
            ps.setInt(3, horaCatedraId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
