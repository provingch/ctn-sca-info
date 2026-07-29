package ctn.informatica.sca.util;

import java.time.LocalDate;

public final class AcademicPeriod {

    private AcademicPeriod() {
        // util class
    }

    public static int current() {
        return LocalDate.now().getYear();
    }
}
