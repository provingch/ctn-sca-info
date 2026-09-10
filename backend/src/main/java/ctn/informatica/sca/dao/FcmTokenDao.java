package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** FCM device tokens for the native parent app. */
@Repository
public class FcmTokenDao extends conexion {

    /** Insert or refresh a token, (re)binding it to the given user. */
    public void upsert(int userId, String userType, String token, String platform) throws SQLException {
        if (token == null || token.isBlank()) {
            return;
        }
        String sql = "INSERT INTO fcm_token (user_id, user_type, token, platform, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), user_type = VALUES(user_type), "
                + "platform = VALUES(platform), updated_at = NOW()";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, userType == null || userType.isBlank() ? "padre" : userType);
            ps.setString(3, token);
            ps.setString(4, platform == null || platform.isBlank() ? "android" : platform);
            ps.executeUpdate();
        }
    }

    public boolean deleteByToken(String token) throws SQLException {
        try (Connection c = getCon();
                PreparedStatement ps = c.prepareStatement("DELETE FROM fcm_token WHERE token = ?")) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        }
    }

    public void deleteByTokens(Collection<String> tokens) throws SQLException {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        String placeholders = tokens.stream().map(t -> "?").collect(Collectors.joining(","));
        try (Connection c = getCon();
                PreparedStatement ps = c.prepareStatement("DELETE FROM fcm_token WHERE token IN (" + placeholders + ")")) {
            int i = 1;
            for (String t : tokens) {
                ps.setString(i++, t);
            }
            ps.executeUpdate();
        }
    }

    public List<String> findTokensByUserId(int userId) throws SQLException {
        List<String> tokens = new ArrayList<>();
        try (Connection c = getCon();
                PreparedStatement ps = c.prepareStatement("SELECT token FROM fcm_token WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(rs.getString(1));
                }
            }
        }
        return tokens;
    }

    /** Distinct tokens for a set of parent user ids. */
    public List<String> findTokensByUserIds(Collection<Integer> userIds) throws SQLException {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        try (Connection c = getCon();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT DISTINCT token FROM fcm_token WHERE user_id IN (" + placeholders + ")")) {
            int i = 1;
            for (Integer id : userIds) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(rs.getString(1));
                }
            }
        }
        return new ArrayList<>(tokens);
    }
}
