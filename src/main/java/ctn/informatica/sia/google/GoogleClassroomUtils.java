package ctn.informatica.sia.google;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ctn.informatica.sia.util.AcademicPeriod;

public final class GoogleClassroomUtils {

    private static final Pattern LEVEL_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}])(primero|segundo|tercero|1(?:º|°|ro|er)?|2(?:º|°|do)?|3(?:º|°|ro)?|[123])(?=(?:\\s*|[°º\\-])?[abc]|\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_PATTERN = Pattern.compile("(?:^|[^\\p{L}\\p{N}]|[0-9°º])([abc])(?=\\b|\\s|$)", Pattern.CASE_INSENSITIVE);

    private GoogleClassroomUtils() {
        // util class
    }

    public static Optional<CourseKey> parseCourseKey(String courseName) {
        return parseCourseKey(courseName, null);
    }

    public static Optional<CourseKey> parseCourseKey(String courseName, String room) {
        if (courseName == null || courseName.isBlank()) {
            return Optional.empty();
        }

        String normalizedName = normalize(courseName);

        Integer level = parseLevel(normalizedName);
        String section = parseSection(normalizedName);

        if (level == null || section == null) {
            return Optional.empty();
        }

        String sala = stripLevelAndSection(normalizedName);
        return Optional.of(new CourseKey(level, section, sala, AcademicPeriod.current()));
    }

    public static String normalizeSubjectName(String subjectName) {
        if (subjectName == null || subjectName.isBlank()) {
            return "";
        }
        return normalize(subjectName);
    }

    public static boolean isAllowedClassroomCourse(String courseName) {
        return parseCourseKey(courseName).isPresent();
    }

    public static String extractSpecialtyHint(String courseName, String room) {
        String cleanedName = stripLevelAndSection(courseName);
        String normalizedName = cleanedName.isBlank() ? "" : normalize(cleanedName);
        return normalizedName;
    }

    private static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("[^\\p{Alnum}\\s-]", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRoom(String room) {
        if (room == null || room.isBlank()) {
            return "";
        }
        return normalize(room);
    }

    private static String stripLevelAndSection(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String withoutLevel = LEVEL_PATTERN.matcher(text).replaceAll(" ");
        String withoutSection = SECTION_PATTERN.matcher(withoutLevel).replaceAll(" ");
        return withoutSection.replaceAll("\\s+", " ").trim();
    }

    public static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String normalized = normalize(title);
        return normalized
                .replaceAll("\\btrabajo practico\\b", "trabajo practico")
                .replaceAll("\\btp\\b", "tp")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean containsNormalizedPhrase(String text, String phrase) {
        if (text == null || phrase == null || text.isBlank() || phrase.isBlank()) {
            return false;
        }
        String normalizedText = normalize(text);
        String normalizedPhrase = normalize(phrase);
        if (normalizedText.contains(normalizedPhrase)) {
            return true;
        }
        String[] tokens = normalizedPhrase.split("\\s+");
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            if (!normalizedText.contains(token)) {
                return false;
            }
        }
        return tokens.length > 0;
    }

    private static Integer parseLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = LEVEL_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase(Locale.ROOT);
            switch (token) {
                case "primero":
                case "1ro":
                case "1er":
                case "1":
                    return 1;
                case "segundo":
                case "2do":
                case "2":
                    return 2;
                case "tercero":
                case "3ro":
                case "3":
                    return 3;
                default:
                    break;
            }
        }
        return null;
    }

    private static String parseSection(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = SECTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token != null) {
                String normalizedToken = token.toUpperCase(Locale.ROOT);
                if (normalizedToken.equals("A") || normalizedToken.equals("B") || normalizedToken.equals("C")) {
                    return normalizedToken;
                }
            }
        }
        return null;
    }

    public static final class CourseKey {
        private final int nivel;
        private final String seccion;
        private final String sala;
        private final int periodo;

        public CourseKey(int nivel, String seccion, String sala) {
            this(nivel, seccion, sala, AcademicPeriod.current());
        }

        public CourseKey(int nivel, String seccion, String sala, int periodo) {
            this.nivel = nivel;
            this.seccion = seccion;
            this.sala = sala == null ? "" : sala;
            this.periodo = periodo;
        }

        public int getNivel() {
            return nivel;
        }

        public String getSeccion() {
            return seccion;
        }

        public String getSala() {
            return sala;
        }

        public int getPeriodo() {
            return periodo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nivel, seccion, sala, periodo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CourseKey other = (CourseKey) obj;
            return nivel == other.nivel && periodo == other.periodo && seccion.equals(other.seccion) && Objects.equals(sala, other.sala);
        }

        @Override
        public String toString() {
            return "CursoKey{nivel=" + nivel + ", seccion='" + seccion + "', sala='" + sala + "', periodo=" + periodo + "}";
        }
    }
}
