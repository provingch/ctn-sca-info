package ctn.informatica.sca.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PasswordUtilTest {

    @Test
    void matchesDoesNotThrowForModernBcryptHashes() {
        String hash = "$2b$12$Rpgbwloez26OOLWlR10gPeyrYdzB8FjZVWi3zlY.fIjbez.tRWXdO";

        assertDoesNotThrow(() -> PasswordUtil.matches("password", hash));
        assertFalse(PasswordUtil.matches("password", hash));
    }
}
