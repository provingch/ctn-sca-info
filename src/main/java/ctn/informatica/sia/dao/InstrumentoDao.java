/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.Instrumento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jonat
 */
// package com.yourcompany.dao;

public class InstrumentoDao extends conexion {

    /**
     * Returns all instrumentos ordered by nombre.
     */
    public List<Instrumento> findAll() throws SQLException {
        List<Instrumento> list = new ArrayList<>();
        String sql = "SELECT id, nombre FROM instrumento ORDER BY nombre";

        try (Connection con = getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Instrumento ins = new Instrumento();
                ins.setId(rs.getInt("id"));
                ins.setNombre(rs.getString("nombre"));
                list.add(ins);
            }
        }

        return list;
    }
}
