package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class AcademicPeriodTest {

    @Test
    void currentEtapa_shouldMatchTransitionRule() {
        LocalDate today = LocalDate.now();
        LocalDate transition = LocalDate.of(today.getYear(), 7, 15);
        int expected = today.isBefore(transition) ? 1 : 2;

        assertEquals(expected, AcademicPeriod.currentEtapa());
    }
}
