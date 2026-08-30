package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HorarioTemplateStylesTest {

    @Test
    void loadsTemplateStylesFromWorkbookResource() {
        HorarioTemplateStyles styles = HorarioTemplateStyles.load();

        assertNotNull(styles);
        assertEquals("Calibri", styles.title().fontName());
        assertEquals(13, styles.title().fontSizePt());
        assertEquals("404040", styles.headerDay().fillColorHex());
        assertEquals("FFFFFF", styles.headerDay().fontColorHex());
        assertEquals("D9D9D9", styles.salasLabel().fillColorHex());
        assertEquals("BFBFBF", styles.receso().fillColorHex());
        assertTrue(styles.hourColumnWidth() > 0);
        assertTrue(styles.dayColumnWidth() > 0);
    }
}
