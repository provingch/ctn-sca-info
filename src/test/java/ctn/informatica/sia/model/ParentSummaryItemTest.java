package ctn.informatica.sia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ParentSummaryItemTest {

    @Test
    public void shouldMapPercentageToGradeScale() {
        ParentSummaryItem item = new ParentSummaryItem();
        item.setPuntos(45);
        item.setTotalPosible(50);
        item.recomputeDerivedValues();

        assertEquals(4, item.getNota());
        assertEquals(90, item.getPorcentaje());
    }
}
