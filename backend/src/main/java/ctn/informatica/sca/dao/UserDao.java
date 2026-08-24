/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.PasswordUtil;

/**
 *
 * @author jonat
 */
@Repository
public class UserDao {

    private static final int PARENT_LEVEL = 4;

    // Returns a User if credentials match, otherwise null
    public User findByUsernameAndPassword(String username, String password) throws Exception {
        String normalizedUsername = username == null ? "" : username.trim();
        User professorUser = findProfessorUser(normalizedUsername, password);
        if (professorUser != null) {
            return professorUser;
        }
        return findParentUser(normalizedUsername, password);
    }

    public List<User> findAllByLevel(int level) throws Exception {
        String sql = "select * from usuario where nivel = ?";
        List<User> out = new ArrayList<>();
        try (Connection con = new conexion().getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, level);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    out.add(mapUser(rs));
                }
            }
        } catch (SQLException ex) {
            throw new Exception("DB connection/query error", ex);
        }
        return out;
    }

    public User findById(int id) throws Exception {
        User professorUser = findProfessorUserById(id);
        if (professorUser != null) {
            return professorUser;
        }
        return findParentUserById(id);
    }

    public User findByIdAndLevel(int id, int level) throws Exception {
        if (level == PARENT_LEVEL) {
            return findParentUserById(id);
        }
        User user = findProfessorUserById(id);
        if (user != null && user.getLevel() == level) {
            return user;
        }
        return null;
    }

    public String findActivityLogPathById(int userId) throws SQLException {
        String sql = "SELECT activity_log_path FROM usuario WHERE id = ?";
        try (Connection con = new conexion().getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("activity_log_path");
                }
            }
        }
        return null;
    }

    public boolean updateActivityLogPath(int userId, String path) throws SQLException {
        String sql = "UPDATE usuario SET activity_log_path = ? WHERE id = ?";
        try (Connection con = new conexion().getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setString(1, path);
            stm.setInt(2, userId);
            return stm.executeUpdate() > 0;
        }
    }

    private User findProfessorUser(String username, String password) throws Exception {
        String sql = "select * from usuario where nivel <> 4 and (usuario = ? OR ci = ?)";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {

            stm.setString(1, username);
            try {
                int ciVal = Integer.parseInt(username);
                stm.setInt(2, ciVal);
            } catch (NumberFormatException ex) {
                stm.setNull(2, java.sql.Types.INTEGER);
            }

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
        String sql = "select * from usuario where nivel <> 4 and id = ?";
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
        String sql = "select id, nombre, apellido, usuario, contrasenia, nivel from usuario where nivel = 4 and (usuario = ? OR ci = ?)";
        try (Connection con = new conexion().getCon();
                PreparedStatement stm = con.prepareStatement(sql)) {

            stm.setString(1, username);
            try {
                int ciVal = Integer.parseInt(username);
                stm.setInt(2, ciVal);
            } catch (NumberFormatException ex) {
                stm.setNull(2, java.sql.Types.INTEGER);
            }

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
        String sql = "select id, nombre, apellido, usuario, contrasenia, nivel from usuario where nivel = 4 and id = ?";
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
