package ctn.informatica.sca.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParentTaskGradeTest {
    private static final LocalDate HOY = LocalDate.of(2026, 8, 16);

    @Test
    void ceroRegistradoSigueSiendoUnaCalificacion() {
        assertEquals(ParentTaskGrade.CALIFICADA,
                ParentTaskGrade.resolveEstado(true, 0, true, HOY.minusDays(1), HOY));
    }

    @Test
    void entregaClassroomSinNotaQuedaPendienteDeCalificacion() {
        assertEquals(ParentTaskGrade.ENTREGADA_PENDIENTE,
                ParentTaskGrade.resolveEstado(true, null, true, HOY.minusDays(1), HOY));
    }

    @Test
    void tareaVencidaSinPuntajeSeMarcaNoEntregada() {
        assertEquals(ParentTaskGrade.NO_ENTREGADA,
                ParentTaskGrade.resolveEstado(false, null, true, HOY.minusDays(1), HOY));
    }

    @Test
    void tareaVigenteSinEntregaPermanecePendiente() {
        assertEquals(ParentTaskGrade.PENDIENTE,
                ParentTaskGrade.resolveEstado(false, null, true, HOY.plusDays(1), HOY));
    }
}
