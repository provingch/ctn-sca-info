package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RefreshTokenDaoRaceTest {

    @Test
    void concurrentRotateReturnsFalseForSecondWinnerWithoutThrowing() throws Exception {
        String oldHash = "race-old-hash";
        String winnerHash = "race-new-hash";
        String loserHash = "race-second-hash";

        AtomicInteger updateCalls = new AtomicInteger();
        AtomicInteger insertCalls = new AtomicInteger();

        RefreshTokenDao dao = new RefreshTokenDao() {
            @Override
            public Connection getCon() throws java.sql.SQLException {
                Connection connection = mock(Connection.class);
                when(connection.getAutoCommit()).thenReturn(true);

                PreparedStatement updateStatement = mock(PreparedStatement.class);
                when(updateStatement.executeUpdate()).thenAnswer(invocation -> {
                    int call = updateCalls.getAndIncrement();
                    return call == 0 ? 1 : 0;
                });

                PreparedStatement insertStatement = mock(PreparedStatement.class);
                when(insertStatement.executeUpdate()).thenAnswer(invocation -> {
                    insertCalls.getAndIncrement();
                    return 1;
                });

                when(connection.prepareStatement("UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP, replaced_by_hash = ? WHERE token_hash = ? AND revoked_at IS NULL"))
                        .thenReturn(updateStatement);
                when(connection.prepareStatement("INSERT INTO refresh_token (token_hash, user_id, user_level, expires_at, user_agent, ip_address) "
                        + "SELECT ?, user_id, user_level, ?, ?, ? FROM refresh_token WHERE token_hash = ? LIMIT 1"))
                        .thenReturn(insertStatement);
                return connection;
            }
        };

        boolean first = assertDoesNotThrow(() -> dao.rotate(oldHash, winnerHash, Instant.now().plusSeconds(60), "ua", "127.0.0.1"));
        boolean second = assertDoesNotThrow(() -> dao.rotate(oldHash, loserHash, Instant.now().plusSeconds(60), "ua", "127.0.0.1"));

        assertTrue(first);
        assertFalse(second);
        assertTrue(insertCalls.get() >= 1);
    }
}
