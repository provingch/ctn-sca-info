package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ScaUiContextTest {

    @Test
    void normalizeSpecialtyConvertsLabelsToCanonicalTokens() {
        assertEquals("mecanica-automotriz", ScaUiContext.normalizeSpecialty("Mecánica Automotriz"));
        assertEquals("general", ScaUiContext.normalizeSpecialty(null));
        assertEquals("general", ScaUiContext.normalizeSpecialty("   "));
        assertEquals("quimica", ScaUiContext.normalizeSpecialty("quimica"));
        assertEquals("construcciones", ScaUiContext.normalizeSpecialty("Construcciones Civiles"));
        assertEquals("quimica", ScaUiContext.normalizeSpecialty("Química Industrial"));
    }
}
