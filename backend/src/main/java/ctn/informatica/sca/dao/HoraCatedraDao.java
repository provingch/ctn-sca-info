package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.HoraCatedra;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class HoraCatedraDao extends conexion {

    private HoraCatedra fromResultSet(ResultSet rs) throws SQLException {
        HoraCatedra hora = new HoraCatedra();
        hora.setId(rs.getInt("id"));
        hora.setNumero(rs.getInt("numero"));
        hora.setEtiqueta(rs.getString("etiqueta"));
        Time horaInicio = rs.getTime("hora_inicio");
        Time horaFin = rs.getTime("hora_fin");
        hora.setHoraInicio(horaInicio == null ? null : horaInicio.toLocalTime());
        hora.setHoraFin(horaFin == null ? null : horaFin.toLocalTime());
        return hora;
    }

    public List<HoraCatedra> findAll() throws SQLException {
        String sql = "SELECT id, numero, etiqueta, hora_inicio, hora_fin FROM hora_catedra ORDER BY numero";
        List<HoraCatedra> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(fromResultSet(rs));
            }
        }
        return out;
    }

    public int crear(int numero, String etiqueta, LocalTime horaInicio, LocalTime horaFin) throws SQLException {
        String sql = "INSERT INTO hora_catedra (numero, etiqueta, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, numero);
            ps.setString(2, etiqueta);
            ps.setTime(3, Time.valueOf(horaInicio));
            ps.setTime(4, Time.valueOf(horaFin));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean actualizar(int id, int numero, String etiqueta, LocalTime horaInicio, LocalTime horaFin) throws SQLException {
        String sql = "UPDATE hora_catedra SET numero = ?, etiqueta = ?, hora_inicio = ?, hora_fin = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numero);
            ps.setString(2, etiqueta);
            ps.setTime(3, Time.valueOf(horaInicio));
            ps.setTime(4, Time.valueOf(horaFin));
            ps.setInt(5, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM hora_catedra WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
