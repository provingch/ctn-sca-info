package ctn.informatica.sia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class TareaTest {

    @Test
    public void testTooltipTextIncludesDatesAndClassroomLink() {
        Tarea tarea = new Tarea();
        tarea.setTitulo("TP 1");
        tarea.setFechaInicio(LocalDate.of(2026, 2, 1));
        tarea.setFechaLimite(LocalDate.of(2026, 7, 15));
        tarea.setGoogleCourseworkUrl("https://classroom.google.com/c/123/a/456");

        String tooltip = tarea.getTooltipText();

        assertTrue(tooltip.contains("Inicio"));
        assertTrue(tooltip.contains("Límite"));
        assertTrue(tooltip.contains("classroom.google.com"));
    }

    @Test
    public void shouldClassifyTaskStageByPublicationDateMidJuly() {
        assertEquals(1, Tarea.resolveEtapaIndexByPublicationDate(LocalDate.of(2026, 7, 14)));
        assertEquals(2, Tarea.resolveEtapaIndexByPublicationDate(LocalDate.of(2026, 7, 15)));
        assertEquals(2, Tarea.resolveEtapaIndexByPublicationDate(LocalDate.of(2026, 7, 16)));
    }
}
