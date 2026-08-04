/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 * @author jonat
 */
public class UserDao {

    private static final int PARENT_LEVEL = 4;

    // Returns a User if credentials match, otherwise null
    public User findByUsernameAndPassword(String username, String password) throws Exception {
        User professorUser = findProfessorUser(username, password);
        if (professorUser != null) {
            return professorUser;
        }
        return findParentUser(username, password);
    }

    public User findById(int id) throws Exception {
        User professorUser = findProfessorUserById(id);
        if (professorUser != null) {
            return professorUser;
        }
        return findParentUserById(id);
    }

    private User findProfessorUser(String username, String password) throws Exception {
        String sql = "select * from profesor where usuario = ?";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {

            stm.setString(1, username);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String stored = rs.getString("contrasenia");
                boolean ok;
                if (PasswordUtil.isBcryptHash(stored)) {
                    ok = PasswordUtil.matches(password, stored);
                } else {
                    // Migración transparente: todavía es texto plano
                    ok = PasswordUtil.matches(password, stored);
                    if (ok) {
                        // Rehashear en el próximo login
                        int userId = rs.getInt("id");
                        try {
                            new ProfesorDao().resetPassword(userId, password);
                        } catch (Exception e) {
                            // Log pero no falla el login
                            System.err.println("Warning: could not rehash password on login for profesor " + userId + ": " + e.getMessage());
                        }
                    }
                }
                if (!ok) {
                    return null;
                }

                return mapUser(rs);
            }
        } catch (java.sql.SQLException ex) {
            throw new Exception("DB connection/query error", ex);
        }
    }

    private User findProfessorUserById(int id) throws Exception {
        String sql = "select * from profesor where id = ?";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, id);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (java.sql.SQLException ex) {
            throw new Exception("DB connection/query error", ex);
        }
        return null;
    }

    private User findParentUser(String username, String password) throws Exception {
        String sql = "select id, nombre, apellido, usuario, contrasenia from padre where usuario = ?";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {

            stm.setString(1, username);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String stored = rs.getString("contrasenia");
                boolean ok;
                if (PasswordUtil.isBcryptHash(stored)) {
                    ok = PasswordUtil.matches(password, stored);
                } else {
                    // Migración transparente: todavía es texto plano
                    ok = PasswordUtil.matches(password, stored);
                    if (ok) {
                        // Rehashear en el próximo login
                        int userId = rs.getInt("id");
                        try {
                            new PadreDao().resetPassword(userId, password);
                        } catch (Exception e) {
                            // Log pero no falla el login
                            System.err.println("Warning: could not rehash password on login for padre " + userId + ": " + e.getMessage());
                        }
                    }
                }
                if (!ok) {
                    return null;
                }

                return mapParentUser(rs);
            }
        } catch (java.sql.SQLException ex) {
            throw new Exception("DB connection/query error", ex);
        }
    }

    private User findParentUserById(int id) throws Exception {
        String sql = "select id, nombre, apellido, usuario, contrasenia from padre where id = ?";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, id);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return mapParentUser(rs);
                }
            }
        } catch (java.sql.SQLException ex) {
            throw new Exception("DB connection/query error", ex);
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws java.sql.SQLException {
        int id = rs.getInt("id");
        String user = rs.getString("usuario");
        String firstName = rs.getString("nombre");
        String lastName = rs.getString("apellido");
        String fullName = (firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())
                ? user
                : ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        int level = rs.getInt("nivel");
        return new User(id, user, fullName, level);
    }

    private User mapParentUser(ResultSet rs) throws java.sql.SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("usuario");
        String fullName = ((rs.getString("nombre") == null ? "" : rs.getString("nombre")) + " "
                + (rs.getString("apellido") == null ? "" : rs.getString("apellido"))).trim();
        return new User(id, username, fullName, PARENT_LEVEL);
    }
}
