package ctn.informatica.sca.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
