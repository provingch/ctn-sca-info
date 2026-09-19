package ctn.informatica.sca.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PlanillaTest {

    @Test
    public void testComputeGradeRanges_smallTotal_points3_exigencia70_communal() {
        Planilla p = new Planilla();
        p.setCategoria("comun");
        p.computeGradeRanges(3);

        // perfect score should map to 5
        assertEquals(5, p.getNotaForSum(3), "Perfect score (3/3) debe dar nota 5");

        // verify no overlapping ranges: for any pair of grades, if both ranges valid they must not overlap
        Map<Integer, int[]> ranges = p.getGradeRanges();
        for (int g1 = 2; g1 <= 5; g1++) {
            for (int g2 = g1 + 1; g2 <= 5; g2++) {
                int[] r1 = ranges.get(g1);
                int[] r2 = ranges.get(g2);
                if (r1 == null || r2 == null) continue;
                int s1 = r1[0], e1 = r1[1];
                int s2 = r2[0], e2 = r2[1];
                if (s1 <= e1 && s2 <= e2) {
                    // must not overlap
                    boolean disjoint = (e1 < s2) || (e2 < s1);
                    assertTrue(disjoint, "Rangos solapados entre notas " + g1 + " y " + g2);
                }
            }
        }
    }

    @Test
    public void testComputeGradeRanges_largeTotal_keepsBalance() {
        Planilla p = new Planilla();
        p.setCategoria("comun");
        p.computeGradeRanges(20);

        // top score should be reachable as 5
        assertEquals(5, p.getNotaForSum(20), "Top score debe mapear a nota 5");

        // ensure total covered points equals inclusiveCount (sum of sizes of non-empty buckets)
        int li = p.getLimiteInferior();
        int ls = p.getLimiteSuperior();
        int inclusive = ls - li + 1;
        int counted = 0;
        for (int g = 2; g <= 5; g++) {
            int[] r = p.getGradeRanges().get(g);
            if (r == null) continue;
            int s = r[0], e = r[1];
            if (s <= e) counted += (e - s + 1);
        }
        assertEquals(inclusive, counted, "La suma de tamaños de buckets debe cubrir el intervalo inclusivo");
    }

    @Test
    public void testComputeGradeRanges_zeroTotal_returnsAllOnes() {
        Planilla p = new Planilla();
        p.setCategoria("comun");
        p.computeGradeRanges(0);

        // with zero total points, any sum (0) should return 1 per existing logic
        assertEquals(1, p.getNotaForSum(0));
    }

    /**
     * Escala oficial del 70% (Servicio de Evaluación), 10..150 puntos.
     * Columnas: TP, li, fin nota 2, inicio nota 3, fin nota 3, inicio nota 4, fin nota 4, inicio nota 5, fin nota 5.
     */
    @ParameterizedTest(name = "TP={0}")
    @CsvSource({
        "12,   8,  8,  9, 10, 11, 11, 12,  12",
        "13,   9,  9, 10, 11, 12, 12, 13,  13",
        "14,  10, 10, 11, 12, 13, 13, 14,  14",
        "15,  11, 11, 12, 13, 14, 14, 15,  15",
        "16,  11, 11, 12, 13, 14, 15, 16,  16",
        "17,  12, 12, 13, 14, 15, 16, 17,  17",
        "19,  13, 14, 15, 16, 17, 18, 19,  19",
        "20,  14, 15, 16, 17, 18, 19, 20,  20",
        "21,  15, 16, 17, 18, 19, 20, 21,  21",
        "26,  18, 19, 20, 22, 23, 24, 25,  26",
        "33,  23, 25, 26, 28, 29, 31, 32,  33",
        "50,  35, 38, 39, 42, 43, 46, 47,  50",
        "60,  42, 46, 47, 51, 52, 56, 57,  60",
        "100, 70, 77, 78, 85, 86, 93, 94, 100"
    })
    public void testComputeGradeRanges_escalaOficial70(int tp, int li,
            int e2, int s3, int e3, int s4, int e4, int s5, int e5) {
        for (String categoria : new String[]{"comun", "especifico"}) {
            Planilla p = new Planilla();
            p.setCategoria(categoria);
            p.computeGradeRanges(tp);

            assertEquals(0.7, p.getExigencia(), 1e-9, "Exigencia fija 70% (" + categoria + ")");
            assertEquals(li, p.getLimiteInferior());
            assertArrayEquals(new int[]{li, e2}, p.getGradeRanges().get(2), "nota 2, TP=" + tp);
            assertArrayEquals(new int[]{s3, e3}, p.getGradeRanges().get(3), "nota 3, TP=" + tp);
            assertArrayEquals(new int[]{s4, e4}, p.getGradeRanges().get(4), "nota 4, TP=" + tp);
            assertArrayEquals(new int[]{s5, e5}, p.getGradeRanges().get(5), "nota 5, TP=" + tp);
            assertEquals(5, p.getNotaForSum(tp), "El puntaje máximo debe dar nota 5, TP=" + tp);
            assertEquals(1, p.getNotaForSum(li - 1), "Debajo de li debe dar nota 1, TP=" + tp);
        }
    }

    /** Fuera de 10..150 se mantiene el cálculo anterior (ceil + resto a la nota más alta). */
    @ParameterizedTest(name = "TP={0} (respaldo)")
    @CsvSource({
        // TP=5: li=ceil(3.5)=4, tamaño 2 -> base 0, rem 2 -> c5=1, c4=1
        "5,    4, 3, 4, 3, 4, 4, 5,  5",
        // TP=200: li=140, tamaño 61 -> base 15, rem 1 -> c5=16
        "200, 140, 154, 155, 169, 170, 184, 185, 200"
    })
    public void testComputeGradeRanges_fueraDeEscala_usaRespaldo(int tp, int li,
            int e2, int s3, int e3, int s4, int e4, int s5, int e5) {
        Planilla p = new Planilla();
        p.setCategoria("comun");
        p.computeGradeRanges(tp);

        assertEquals(li, p.getLimiteInferior());
        assertArrayEquals(new int[]{li, e2}, p.getGradeRanges().get(2), "nota 2, TP=" + tp);
        assertArrayEquals(new int[]{s3, e3}, p.getGradeRanges().get(3), "nota 3, TP=" + tp);
        assertArrayEquals(new int[]{s4, e4}, p.getGradeRanges().get(4), "nota 4, TP=" + tp);
        assertArrayEquals(new int[]{s5, e5}, p.getGradeRanges().get(5), "nota 5, TP=" + tp);
    }
}
