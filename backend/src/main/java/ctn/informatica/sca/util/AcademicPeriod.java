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
        return etapaAt(LocalDate.now());
    }

    public static int etapaAt(LocalDate date) {
        if (date == null) {
            return 1;
        }
        return date.isBefore(etapaStartDate(date.getYear(), 2)) ? 1 : 2;
    }

    public static LocalDate etapaStartDate(int year, int etapa) {
        return switch (etapa) {
            case 1 -> LocalDate.of(year, 2, 23);
            case 2 -> LocalDate.of(year, 6, 22);
            default -> throw new IllegalArgumentException("La etapa debe ser 1 o 2");
        };
    }
}
