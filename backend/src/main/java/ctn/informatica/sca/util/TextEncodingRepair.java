package ctn.informatica.sca.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/** Repairs known UTF-8 sequences read as Latin-1/Windows-1252, never whole strings. */
public final class TextEncodingRepair {
    private static final Map<String, String> REPLACEMENTS = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
    static {
        // Restricted to Spanish accents and common punctuation. Correct Unicode, emoji,
        // replacement characters and unknown sequences are deliberately left untouched.
        String characters = "áéíóúÁÉÍÓÚñÑüÜ¿¡°ºª«»–—‘’“”…";
        for (int codePoint : characters.codePoints().toArray()) {
            String correct = new String(Character.toChars(codePoint));
            byte[] bytes = correct.getBytes(StandardCharsets.UTF_8);
            for (Charset charset : new Charset[]{StandardCharsets.ISO_8859_1, Charset.forName("windows-1252")}) {
                String broken = new String(bytes, charset);
                if (!broken.contains("\uFFFD")) {
                    REPLACEMENTS.put(broken, correct);
                    for (Charset second : new Charset[]{StandardCharsets.ISO_8859_1, Charset.forName("windows-1252")}) {
                        String twice = new String(broken.getBytes(StandardCharsets.UTF_8), second);
                        if (!twice.contains("\uFFFD")) REPLACEMENTS.put(twice, correct);
                    }
                }
            }
        }
    }

    private TextEncodingRepair() {}

    public static String repair(String value) {
        if (value == null) return null;
        String result = value;
        for (int pass = 0; pass < 4; pass++) {
            String previous = result;
            for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
            if (result.equals(previous)) break;
        }
        return result;
    }
}
