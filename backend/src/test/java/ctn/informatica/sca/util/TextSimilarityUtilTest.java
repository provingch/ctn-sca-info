package ctn.informatica.sca.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TextSimilarityUtilTest {

    @Test
    public void identicalTexts_shouldReturnOne() {
        String a = "Introducción a la programación";
        String b = "Introducción a la programación";
        assertEquals(1.0, TextSimilarityUtil.similarity(a,b), 1e-9);
    }

    @Test
    public void paraphraseWithKeywords_shouldBeAboveThreshold() {
        String a = "Repaso: estructuras de control y bucles for/while";
        String b = "Estructuras de control: bucles y manejo de flujo";
        double sim = TextSimilarityUtil.similarity(a,b);
        assertTrue(sim >= 0.35, "Expected similarity >= 0.35 but was " + sim);
    }

    @Test
    public void unrelatedTexts_shouldBeLow() {
        String a = "Historia del arte renacentista";
        String b = "Algoritmos y estructuras de datos";
        double sim = TextSimilarityUtil.similarity(a,b);
        assertTrue(sim < 0.2, "Expected similarity < 0.2 but was " + sim);
    }

    @Test
    public void emptyOrNull_shouldReturnZero() {
        assertEquals(0.0, TextSimilarityUtil.similarity(null, "algo"), 1e-9);
        assertEquals(0.0, TextSimilarityUtil.similarity("", ""), 1e-9);
        assertEquals(0.0, TextSimilarityUtil.similarity("   ", "palabra"), 1e-9);
    }
}
