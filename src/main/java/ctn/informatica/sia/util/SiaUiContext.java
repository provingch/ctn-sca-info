package ctn.informatica.sia.util;

import java.util.Locale;

public final class SiaUiContext {

    private SiaUiContext() {
    }

    public static String normalizeSpecialty(String value) {
        if (value == null) {
            return "informatica";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "informatica";
        }
        return switch (normalized) {
            case "mecánica automotriz", "mecanica automotriz", "mecánica-automotriz", "mecanica-automotriz" -> "mecanica-automotriz";
            case "mecánica general", "mecanica general", "mecánica-general", "mecanica-general" -> "mecanica-general";
            case "electromecánica", "electromecanica" -> "electromecanica";
            case "electricidad" -> "electricidad";
            case "electrónica", "electronica" -> "electronica";
            case "construcciones civiles", "construcciones-civiles", "construcciones" -> "construcciones";
            case "química industrial", "quimica industrial", "química-industrial", "quimica-industrial", "química", "quimica" -> "quimica";
            case "informática", "informatica" -> "informatica";
            default -> normalized.replace(' ', '-').replace('_', '-');
        };
    }
}
