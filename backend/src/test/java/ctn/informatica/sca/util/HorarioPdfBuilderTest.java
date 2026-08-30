package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class HorarioPdfBuilderTest {

    @Test
    void resolvesSpecialtyAccentForRecesoInPdf() {
        assertEquals("#7a1f2b", HorarioPdfBuilder.resolveRecesoFillHex("Informática"));
        assertNotEquals("BFBFBF", HorarioPdfBuilder.resolveRecesoFillHex("Informática"));
    }
}
