/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author jonat
 */
public class GradeDao extends conexion {
    /**
     * grades: registroId -> (tareaId -> puntos)
     * The inner Integer value may be null which means the DB value should be SQL NULL.
     */
    public void saveGradesBatch(int planillaId, Map<Integer, Map<Integer, Integer>> grades) throws SQLException {
        if (grades == null || grades.isEmpty()) return;
        String sql = "INSERT INTO puntaje (registro_id, tarea_id, puntos) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE puntos = VALUES(puntos)";
        try (Connection con = getCon();
             PreparedStatement stm = con.prepareStatement(sql)) {
            try {
                con.setAutoCommit(false);
                for (Map.Entry<Integer, Map<Integer, Integer>> rEntry : grades.entrySet()) {
                    int registroId = rEntry.getKey();
                    for (Map.Entry<Integer, Integer> tEntry : rEntry.getValue().entrySet()) {
                        int tareaId = tEntry.getKey();
                        Integer puntos = tEntry.getValue(); // may be null

                        stm.setInt(1, registroId);
                        stm.setInt(2, tareaId);

                        if (puntos == null) {
                            stm.setNull(3, java.sql.Types.INTEGER);
                        } else {
                            stm.setInt(3, puntos);
                        }

                        stm.addBatch();
                    }
                }
                stm.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}