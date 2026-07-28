/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.Tarea;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author jonat
 */
public class TareaDao extends conexion {

    public ArrayList<Tarea> consultarTarea(int planillaId) throws SQLException {
        String sql = "SELECT * FROM tarea WHERE planilla_id = ?";// TODO change order
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, planillaId);
            ResultSet rs = stm.executeQuery();
            ArrayList<Tarea> tareas = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int courseworkIndex = -1;
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if ("google_coursework_id".equalsIgnoreCase(meta.getColumnName(i))) {
                    courseworkIndex = i;
                    break;
                }
            }

            while (rs.next()) {
                int id = rs.getInt("id");
                int planilla_id = rs.getInt("planilla_id");
                int instrumento_id = rs.getInt("instrumento_id");
                LocalDate fecha = rs.getObject("fecha", LocalDate.class);
                int total = rs.getInt("total");
                String titulo = rs.getString("titulo");
                String googleCourseworkId = courseworkIndex > 0 ? rs.getString(courseworkIndex) : null;

                Tarea t = new Tarea(id, planilla_id, instrumento_id, fecha, total, titulo);
                t.setGoogleCourseworkId(googleCourseworkId);
                try {
                    t.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                    t.setFechaLimite(rs.getObject("fecha_limite", LocalDate.class));
                } catch (SQLException ignored) {
                    // columnas opcionales en bases antiguas
                }
                try {
                    t.setGoogleCourseworkUrl(rs.getString("google_coursework_url"));
                } catch (SQLException ignored) {
                    // columna opcional en bases antiguas
                }
                tareas.add(t);
            }
            return tareas;
        }
    }

    public Set<String> getGoogleCourseworkIdsForPlanilla(int planillaId) throws SQLException {
        try (Connection con = getCon()) {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet columns = meta.getColumns(null, null, "tarea", "google_coursework_id")) {
                if (!columns.next()) {
                    return Collections.emptySet();
                }
            }

            String sql = "SELECT google_coursework_id FROM tarea WHERE planilla_id = ? AND google_coursework_id IS NOT NULL";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, planillaId);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<String> ids = new LinkedHashSet<>();
                    while (rs.next()) {
                        String id = rs.getString("google_coursework_id");
                        if (id != null && !id.isBlank()) {
                            ids.add(id);
                        }
                    }
                    return ids;
                }
            }
        }
    }

    public Tarea findById(int id) throws SQLException {
        String sql = "SELECT * FROM tarea WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int planilla_id = rs.getInt("planilla_id");
                    int instrumento_id = rs.getInt("instrumento_id");
                    LocalDate fecha = rs.getObject("fecha", LocalDate.class);
                    int total = rs.getInt("total");
                    String titulo = rs.getString("titulo");
                    String googleCourseworkId = null;
                    try {
                        googleCourseworkId = rs.getString("google_coursework_id");
                    } catch (SQLException ex) {
                        // columna opcional en bases antiguas
                    }
                    Tarea tarea = new Tarea(id, planilla_id, instrumento_id, fecha, total, titulo);
                    tarea.setGoogleCourseworkId(googleCourseworkId);
                    try {
                        tarea.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                        tarea.setFechaLimite(rs.getObject("fecha_limite", LocalDate.class));
                    } catch (SQLException ignored) {
                        // columnas opcionales en bases antiguas
                    }
                    try {
                        tarea.setGoogleCourseworkUrl(rs.getString("google_coursework_url"));
                    } catch (SQLException ignored) {
                        // columna opcional en bases antiguas
                    }
                    return tarea;
                } else {
                    return null;
                }
            }
        }
    }

    public void insertarTarea(Tarea tarea) throws SQLException {
        try {
            insertarTareaConMetadatos(tarea);
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("unknown column") || message.contains("doesn't exist") || message.contains("no such column")) {
                insertarTareaSinMetadatos(tarea);
            } else {
                throw ex;
            }
        }
    }

    private void insertarTareaConMetadatos(Tarea tarea) throws SQLException {
        String sql = "INSERT INTO tarea (planilla_id, instrumento_id, fecha, total, titulo, google_coursework_id, google_coursework_url, fecha_inicio, fecha_limite) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, tarea.getPlanillaId());
            ps.setInt(2, tarea.getInstrumentoId());
            ps.setDate(3, java.sql.Date.valueOf(tarea.getFecha()));
            ps.setInt(4, tarea.getTotal());
            ps.setString(5, tarea.getTitulo());
            ps.setString(6, tarea.getGoogleCourseworkId());
            ps.setString(7, tarea.getGoogleCourseworkUrl());
            ps.setDate(8, tarea.getFechaInicio() != null ? java.sql.Date.valueOf(tarea.getFechaInicio()) : null);
            ps.setDate(9, tarea.getFechaLimite() != null ? java.sql.Date.valueOf(tarea.getFechaLimite()) : null);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Inserting tarea failed, no rows affected.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tarea.setId(keys.getInt(1));
                }
            }
        }
    }

    private void insertarTareaSinMetadatos(Tarea tarea) throws SQLException {
        String sql = "INSERT INTO tarea (planilla_id, instrumento_id, fecha, total, titulo, google_coursework_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tarea.getPlanillaId());
            ps.setInt(2, tarea.getInstrumentoId());
            ps.setDate(3, java.sql.Date.valueOf(tarea.getFecha()));
            ps.setInt(4, tarea.getTotal());
            ps.setString(5, tarea.getTitulo());
            ps.setString(6, tarea.getGoogleCourseworkId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Inserting tarea failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tarea.setId(keys.getInt(1));
                }
            }
        }
    }

//    public void update(Tarea tarea) throws SQLException {
//        String sql = "UPDATE tarea SET planilla_id = ?, instrumento_id = ?, fecha = ?, total = ?, titulo = ? WHERE id = ?";
//        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
//            ps.setInt(1, tarea.getPlanillaId());
//            ps.setInt(2, tarea.getInstrumentoId());
//            ps.setDate(3, java.sql.Date.valueOf(tarea.getFecha()));
//            ps.setInt(4, tarea.getTotal());
//            ps.setString(5, tarea.getTitulo());
//            ps.setInt(6, tarea.getId());
//            int affected = ps.executeUpdate();
//            if (affected == 0) {
//                throw new SQLException("Updating tarea failed, no rows affected.");
//            }
//        }
//    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tarea WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    
    public boolean update(Tarea tarea) throws SQLException {
    String selectSql = "SELECT total FROM tarea WHERE id = ? FOR UPDATE";
    String deleteGradesSql = "DELETE FROM puntaje WHERE tarea_id = ?";

    try (Connection con = getCon()) {
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int oldTotal = -1;
            try (PreparedStatement psSel = con.prepareStatement(selectSql)) {
                psSel.setInt(1, tarea.getId());
                try (ResultSet rs = psSel.executeQuery()) {
                    if (rs.next()) {
                        oldTotal = rs.getInt(1);
                    } else {
                        throw new SQLException("Tarea not found for id " + tarea.getId());
                    }
                }
            }

            try {
                actualizarTareaConMetadatos(con, tarea);
            } catch (SQLException ex) {
                String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if (message.contains("unknown column") || message.contains("doesn't exist") || message.contains("no such column")) {
                    actualizarTareaSinMetadatos(con, tarea);
                } else {
                    throw ex;
                }
            }

            boolean cleared = false;
            if (oldTotal != tarea.getTotal()) {
                try (PreparedStatement psDel = con.prepareStatement(deleteGradesSql)) {
                    psDel.setInt(1, tarea.getId());
                    psDel.executeUpdate();
                    cleared = true;
                }
            }

            con.commit();
            con.setAutoCommit(oldAutoCommit);
            return cleared;
        } catch (SQLException ex) {
            try { con.rollback(); } catch (SQLException ignore) {}
            throw ex;
        }
    }
}

    private void actualizarTareaConMetadatos(Connection con, Tarea tarea) throws SQLException {
        String updateSql = "UPDATE tarea SET planilla_id = ?, instrumento_id = ?, fecha = ?, total = ?, titulo = ?, google_coursework_id = ?, google_coursework_url = ?, fecha_inicio = ?, fecha_limite = ? WHERE id = ?";
        try (PreparedStatement psUpd = con.prepareStatement(updateSql)) {
            psUpd.setInt(1, tarea.getPlanillaId());
            psUpd.setInt(2, tarea.getInstrumentoId());
            psUpd.setDate(3, java.sql.Date.valueOf(tarea.getFecha()));
            psUpd.setInt(4, tarea.getTotal());
            psUpd.setString(5, tarea.getTitulo());
            psUpd.setString(6, tarea.getGoogleCourseworkId());
            psUpd.setString(7, tarea.getGoogleCourseworkUrl());
            psUpd.setDate(8, tarea.getFechaInicio() != null ? java.sql.Date.valueOf(tarea.getFechaInicio()) : null);
            psUpd.setDate(9, tarea.getFechaLimite() != null ? java.sql.Date.valueOf(tarea.getFechaLimite()) : null);
            psUpd.setInt(10, tarea.getId());
            int affected = psUpd.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Updating tarea failed, no rows affected.");
            }
        }
    }

    private void actualizarTareaSinMetadatos(Connection con, Tarea tarea) throws SQLException {
        String updateSql = "UPDATE tarea SET planilla_id = ?, instrumento_id = ?, fecha = ?, total = ?, titulo = ?, google_coursework_id = ? WHERE id = ?";
        try (PreparedStatement psUpd = con.prepareStatement(updateSql)) {
            psUpd.setInt(1, tarea.getPlanillaId());
            psUpd.setInt(2, tarea.getInstrumentoId());
            psUpd.setDate(3, java.sql.Date.valueOf(tarea.getFecha()));
            psUpd.setInt(4, tarea.getTotal());
            psUpd.setString(5, tarea.getTitulo());
            psUpd.setString(6, tarea.getGoogleCourseworkId());
            psUpd.setInt(7, tarea.getId());
            int affected = psUpd.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Updating tarea failed, no rows affected.");
            }
        }
    }
}
