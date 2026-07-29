package ctn.informatica.sca.util;

import ctn.informatica.sca.config.AppConfig;

public final class AcademicPeriod {

    private static final int DEFAULT_PERIOD = 2025;

    private AcademicPeriod() {
        // util class
    }

    public static int current() {
        try {
            return Integer.parseInt(AppConfig.get("academic.period"));
        } catch (RuntimeException ex) {
            return DEFAULT_PERIOD;
        }
    }
}
