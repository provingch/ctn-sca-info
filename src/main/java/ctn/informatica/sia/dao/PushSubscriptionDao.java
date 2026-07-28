package ctn.informatica.sia.dao;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.PushSubscription;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PushSubscriptionDao extends conexion {

    private PushSubscription map(ResultSet rs) throws SQLException {
        PushSubscription subscription = new PushSubscription();
        subscription.setId(rs.getInt("id"));
        subscription.setUserId(rs.getInt("user_id"));
        subscription.setUserType(rs.getString("user_type"));
        subscription.setEndpoint(rs.getString("endpoint"));
        subscription.setP256dh(rs.getString("p256dh"));
        subscription.setAuth(rs.getString("auth"));
        return subscription;
    }

    public boolean save(int userId, String userType, String endpoint, String p256dh, String auth) throws SQLException {
        if (endpoint == null || endpoint.isBlank()) {
            return false;
        }
        try (Connection c = getCon()) {
            c.setAutoCommit(false);
            try {
                deleteByEndpoint(endpoint);
                String sql = "INSERT INTO push_subscription (user_id, user_type, endpoint, p256dh, auth, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, userType == null ? "profesor" : userType);
                    ps.setString(3, endpoint);
                    ps.setString(4, p256dh);
                    ps.setString(5, auth);
                    ps.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public boolean deleteByUser(int userId, String userType) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("DELETE FROM push_subscription WHERE user_id = ? AND user_type = ?")) {
            ps.setInt(1, userId);
            ps.setString(2, userType == null ? "profesor" : userType);
            return ps.executeUpdate() >= 0;
        }
    }

    public boolean deleteByEndpoint(String endpoint) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("DELETE FROM push_subscription WHERE endpoint = ?")) {
            ps.setString(1, endpoint);
            return ps.executeUpdate() >= 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("DELETE FROM push_subscription WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<PushSubscription> findByUser(int userId, String userType) throws SQLException {
        List<PushSubscription> subscriptions = new ArrayList<>();
        String sql = "SELECT id, user_id, user_type, endpoint, p256dh, auth FROM push_subscription WHERE user_id = ? AND user_type = ? ORDER BY id DESC";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, userType == null ? "profesor" : userType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subscriptions.add(map(rs));
                }
            }
        }
        return subscriptions;
    }

    public List<PushSubscription> findAll() throws SQLException {
        List<PushSubscription> subscriptions = new ArrayList<>();
        String sql = "SELECT id, user_id, user_type, endpoint, p256dh, auth FROM push_subscription ORDER BY id DESC";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subscriptions.add(map(rs));
            }
        }
        return subscriptions;
    }

    public boolean deleteExpired(int olderThanDays) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("DELETE FROM push_subscription WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)")) {
            ps.setInt(1, olderThanDays);
            return ps.executeUpdate() >= 0;
        }
    }
}
