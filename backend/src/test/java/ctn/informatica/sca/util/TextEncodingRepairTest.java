package ctn.informatica.sca.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextEncodingRepairTest {
    @Test void repairsLatin1AndWindows1252IncludingDoubleEncoding() {
        for (Charset charset : new Charset[]{StandardCharsets.ISO_8859_1, Charset.forName("windows-1252")}) {
            String correct = "Electrónica / Mecánica / Muñoz / Matemática / 3° / ¿Qué?";
            String broken = new String(correct.getBytes(StandardCharsets.UTF_8), charset);
            assertEquals(correct, TextEncodingRepair.repair(broken));
            assertEquals(correct, TextEncodingRepair.repair(new String(broken.getBytes(StandardCharsets.UTF_8), charset)));
        }
    }
    @Test void preservesCorrectAndMixedTextAndIsIdempotent() {
        String correct = "Electrónica, Ñandú, João, 中文, 😀, x < y, Ã, �";
        assertEquals(correct, TextEncodingRepair.repair(correct));
        String mixed = "Electr\u00c3\u00b3nica y Matemática";
        String repaired = TextEncodingRepair.repair(mixed);
        assertEquals("Electrónica y Matemática", repaired);
        assertEquals(repaired, TextEncodingRepair.repair(repaired));
        assertNull(TextEncodingRepair.repair(null));
    }
}
