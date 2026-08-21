package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void locksAfterFiveFailuresAndProgressesAfterEachLockExpires() throws Exception {
        LoginAttemptService service = new LoginAttemptService();

        for (int attempt = 1; attempt < 5; attempt++) {
            assertTrue(service.recordFailure("user", "ip") == 0);
        }

        long firstLock = service.recordFailure("user", "ip");
        assertTrue(firstLock >= 29);
        assertTrue(service.blockedSeconds("user") >= 29);

        Thread.sleep(1_100);
        long stillLocked = service.blockedSeconds("user", "ip");
        assertTrue(stillLocked >= 28);
    }

    @Test
    void successfulAuthenticationClearsAllRelatedKeys() {
        LoginAttemptService service = new LoginAttemptService();
        service.recordFailure("user", "ip");
        service.clear("user", "ip");

        assertTrue(service.blockedSeconds("user", "ip") == 0);
    }
}