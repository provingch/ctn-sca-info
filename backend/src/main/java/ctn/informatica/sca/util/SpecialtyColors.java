package ctn.informatica.sca.util;

import java.text.Normalizer;
import java.util.Map;

/**
 * Mapeo simple de especialidad normalizada -> color HEX.
 * Mantener sincronizado con frontend/src/index.css (variables --accent / --hero-tone).
 */
public final class SpecialtyColors {

    private static final Map<String, String> COLORS = Map.of(
        "general", "#5267f7",
        "informatica", "#7a1f2b",
        "construcciones", "#806826",
        "quimica", "#52677a",
        "electronica", "#626975",
        "mecanica-automotriz", "#233c75",
        "mecanica-general", "#1e5a38",
        "electromecanica", "#3347c5",
        "electricidad", "#1f7399"
    );

    public static String getAccent(String specialty) {
        String key = normalizeSpecialty(specialty);
        return COLORS.getOrDefault(key, COLORS.get("general"));
    }

    public static String normalizeSpecialty(String value) {
        if (value == null) return "general";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase().trim().replaceAll("[_\\s]+", "-")
                .replaceAll("[^a-z0-9-]", "").replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        switch (normalized) {
            case "construcciones-civiles":
            case "construccion-civil":
                return "construcciones";
            case "quimica-industrial":
                return "quimica";
            case "mecanica-industrial":
                return "mecanica-general";
            default:
                return normalized.isBlank() ? "general" : normalized;
        }
    }
}
