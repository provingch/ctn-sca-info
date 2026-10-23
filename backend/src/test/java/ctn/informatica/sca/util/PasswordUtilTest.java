package ctn.informatica.sca.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void matchesDoesNotThrowForModernBcryptHashes() {
        String hash = "$2b$12$Rpgbwloez26OOLWlR10gPeyrYdzB8FjZVWi3zlY.fIjbez.tRWXdO";

        assertDoesNotThrow(() -> PasswordUtil.matches("password", hash));
        assertTrue(PasswordUtil.matches("password", hash));
    }

    @Test
    void temporalEsLegibleYCambiaCadaVez() {
        String a = PasswordUtil.temporal();
        assertTrue(a.matches("[a-hjkmnp-z2-9]{10}"), a); // sin 0, o, 1, l, i
        assertNotEquals(a, PasswordUtil.temporal());
    }
}
