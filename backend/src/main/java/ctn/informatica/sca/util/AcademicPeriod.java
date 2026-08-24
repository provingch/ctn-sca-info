package ctn.informatica.sca.util;

import java.time.LocalDate;

public final class AcademicPeriod {

    private AcademicPeriod() {
        // util class
    }

    public static int current() {
        return LocalDate.now().getYear();
    }

    public static int currentEtapa() {
        int currentYear = current();
        LocalDate transition = LocalDate.of(currentYear, 7, 15);
        return LocalDate.now().isBefore(transition) ? 1 : 2;
    }
}
