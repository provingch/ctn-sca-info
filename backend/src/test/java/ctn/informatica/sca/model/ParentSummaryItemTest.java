package ctn.informatica.sca.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParentSummaryItemTest {

    @Test
    void usaLaExigenciaDeUnaMateriaComun() {
        ParentSummaryItem item = summary("comun", 75, 100);

        assertEquals(75, item.getPorcentaje());
        assertEquals(2, item.getNota());
    }

    @Test
    void usaLaExigenciaMayorDeUnaMateriaEspecifica() {
        ParentSummaryItem item = summary("especifico", 75, 100);

        assertEquals(75, item.getPorcentaje());
        assertEquals(1, item.getNota());
    }

    private ParentSummaryItem summary(String categoria, int puntos, int total) {
        ParentSummaryItem item = new ParentSummaryItem();
        item.setCategoria(categoria);
        item.setPuntos(puntos);
        item.setTotalPosible(total);
        item.recomputeDerivedValues();
        return item;
    }
}
