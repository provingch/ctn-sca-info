package ctn.informatica.sia.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SiaUiContextTest {

    @Test
    void normalizeSpecialtyConvertsLabelsToCanonicalTokens() {
        assertEquals("mecanica-automotriz", SiaUiContext.normalizeSpecialty("Mecánica Automotriz"));
        assertEquals("informatica", SiaUiContext.normalizeSpecialty("   "));
        assertEquals("quimica", SiaUiContext.normalizeSpecialty("quimica"));
        assertEquals("construcciones", SiaUiContext.normalizeSpecialty("Construcciones Civiles"));
        assertEquals("quimica", SiaUiContext.normalizeSpecialty("Química Industrial"));
    }
}
