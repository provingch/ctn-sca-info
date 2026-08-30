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
        assertTrue(styles.title().wrapText());
        assertEquals("Calibri", styles.subtitle().fontName());
        assertEquals(11, styles.subtitle().fontSizePt());
        assertEquals("000000", styles.anioBanner().fontColorHex());
        assertEquals("B9BFC7", styles.anioBanner().fillColorHex());
        assertTrue(styles.anioBanner().bold());
        assertEquals(12, styles.anioBanner().fontSizePt());
        assertEquals("404040", styles.headerDay().fillColorHex());
        assertEquals("FFFFFF", styles.headerDay().fontColorHex());
        assertEquals("D9D9D9", styles.salasLabel().fillColorHex());
        assertEquals("BFBFBF", styles.receso().fillColorHex());
        assertTrue(styles.hourColumnWidth() > 0);
        assertTrue(styles.dayColumnWidth() > 0);
    }
}
