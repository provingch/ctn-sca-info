package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Lectura de las anotaciones que carga la app Android (tablas {@code anotacion} y
 * {@code anotacion_foto}, migración 007) para mostrarlas en la web: coordinación ve las
 * recientes de todo el colegio y cada padre solo las de sus hijos. Solo hace SELECT.
 */
@Repository
public class AnotacionWebDao extends conexion {

    public record Anotacion(int id, int alumnoId, String alumnoNombre, String alumnoApellido, String especialidad,
            Integer promocion, String seccion, LocalDateTime fechaHora, String tipo, String observacion,
            String materia, String profesorNombre, String profesorApellido, Integer fotoId) {
    }

    public record Foto(String tipoMime, byte[] imagen) {
    }

    private static final String SELECT = "SELECT a.id, a.alumno_id, al.nombre AS alumno_nombre, al.apellido AS alumno_apellido, "
            + "e.nombre AS especialidad, c.promocion, c.seccion, a.fecha_hora, a.observacion, "
            + "t.descripcion AS tipo, m.nombre AS materia, u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
            + "(SELECT f.id FROM anotacion_foto f WHERE f.anotacion_id = a.id ORDER BY f.id DESC LIMIT 1) AS foto_id "
            + "FROM anotacion a JOIN tipo_anotacion t ON t.id = a.tipo_id "
            + "JOIN alumno al ON al.id = a.alumno_id "
            + "LEFT JOIN curso c ON c.id = al.curso_id LEFT JOIN especialidad e ON e.id = c.especialidad_id "
            + "LEFT JOIN materia m ON m.id = a.materia_id LEFT JOIN usuario u ON u.id = a.usuario_id ";

    /** Historial completo de un alumno, de la más nueva a la más vieja. */
    public List<Anotacion> porAlumno(int alumnoId) throws SQLException {
        try (Connection con = getCon();
                PreparedStatement ps = con.prepareStatement(SELECT + "WHERE a.alumno_id = ? ORDER BY a.fecha_hora DESC, a.id DESC")) {
            ps.setInt(1, alumnoId);
            return leer(ps);
        }
    }

    /** Las últimas {@code limite} anotaciones de todo el colegio. */
    public List<Anotacion> recientes(int limite) throws SQLException {
        try (Connection con = getCon();
                PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY a.fecha_hora DESC, a.id DESC LIMIT ?")) {
            ps.setInt(1, limite);
            return leer(ps);
        }
    }

    /**
     * Última foto de la anotación. Con {@code alumnoId} solo la devuelve si la anotación es de
     * ese alumno, para que un padre no pueda pedir fotos de otros cambiando el id.
     */
    public Foto foto(int anotacionId, Integer alumnoId) throws SQLException {
        String sql = "SELECT f.tipo_mime, f.imagen FROM anotacion_foto f JOIN anotacion a ON a.id = f.anotacion_id "
                + "WHERE f.anotacion_id = ?" + (alumnoId != null ? " AND a.alumno_id = ?" : "") + " ORDER BY f.id DESC LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anotacionId);
            if (alumnoId != null) ps.setInt(2, alumnoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Foto(rs.getString("tipo_mime"), rs.getBytes("imagen")) : null;
            }
        }
    }

    private static List<Anotacion> leer(PreparedStatement ps) throws SQLException {
        List<Anotacion> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Anotacion(rs.getInt("id"), rs.getInt("alumno_id"), rs.getString("alumno_nombre"),
                        rs.getString("alumno_apellido"), rs.getString("especialidad"), rs.getObject("promocion", Integer.class),
                        rs.getString("seccion"), rs.getObject("fecha_hora", LocalDateTime.class), rs.getString("tipo"),
                        rs.getString("observacion"), rs.getString("materia"), rs.getString("profesor_nombre"),
                        rs.getString("profesor_apellido"), rs.getObject("foto_id", Integer.class)));
            }
        }
        return out;
    }
}
