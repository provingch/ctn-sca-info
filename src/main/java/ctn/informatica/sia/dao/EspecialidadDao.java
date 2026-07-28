/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jonat
 */
public class EspecialidadDao extends conexion {

    public EspecialidadDao() {
    }

    public List<Especialidad> findAll() throws Exception {
        String sql = "SELECT id, nombre FROM especialidad ORDER BY nombre";
        List<Especialidad> list = new ArrayList<>();

        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Especialidad e = new Especialidad(rs.getInt("id"), rs.getString("nombre"));
                list.add(e);
            }
        }
        return list;
    }

    public Especialidad findById(int id) throws Exception {
        String sql = "SELECT id, nombre FROM especialidad WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Especialidad(rs.getInt("id"), rs.getString("nombre"));
                }
            }
        }
        return null;
    }

    public Especialidad findByName(String name) throws Exception {
        String sql = "SELECT id, nombre FROM especialidad WHERE LOWER(nombre) = LOWER(?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Especialidad(rs.getInt("id"), rs.getString("nombre"));
                }
            }
        }
        return null;
    }
}
