package ctn.informatica.sca.util;

public final class StudentLoadPolicy {

    public static final int DEFAULT_TARGET = 28;

    private StudentLoadPolicy() {
    }

    public static int getTargetCapacity() {
        return DEFAULT_TARGET;
    }

    public static String getCapacityStatus(int currentCount, int target) {
        if (currentCount < target) {
            return "warning";
        }
        if (currentCount > target) {
            return "over";
        }
        return "ideal";
    }

    public static String getCapacityMessage(int currentCount, int target) {
        int delta = currentCount - target;
        if (delta == 0) {
            return "Curso dentro de la meta de " + target + " alumnos.";
        }
        if (delta > 0) {
            return "Curso por encima de la meta de " + target + " alumnos (excede " + Math.abs(delta) + ").";
        }
        return "Curso por debajo de la meta de " + target + " alumnos (falta " + Math.abs(delta) + ").";
    }
}
