package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class AcademicPeriodTest {

    @Test
    void currentEtapa_shouldMatchTransitionRule() {
        LocalDate today = LocalDate.now();
        LocalDate transition = LocalDate.of(today.getYear(), 6, 22);
        int expected = today.isBefore(transition) ? 1 : 2;

        assertEquals(expected, AcademicPeriod.currentEtapa());
    }

    @Test
    void etapaStartDate_shouldReturnConfiguredAcademicDates() {
        assertEquals(LocalDate.of(2026, 2, 23), AcademicPeriod.etapaStartDate(2026, 1));
        assertEquals(LocalDate.of(2026, 6, 22), AcademicPeriod.etapaStartDate(2026, 2));
    }

    @Test
    void etapaAt_shouldChangeOnSecondStageStartDate() {
        assertEquals(1, AcademicPeriod.etapaAt(LocalDate.of(2026, 6, 21)));
        assertEquals(2, AcademicPeriod.etapaAt(LocalDate.of(2026, 6, 22)));
    }
}
