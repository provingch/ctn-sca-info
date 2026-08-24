package ctn.informatica.sca.util;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

public class TextSimilarityUtil {
    private static final Set<String> STOPWORDS = Set.of(
        "de","la","el","los","las","en","y","a","del","con","para","un","una","que","su","al"
    );

    public static double similarity(String a, String b) {
        Set<String> tokensA = normalizeToTokens(a);
        Set<String> tokensB = normalizeToTokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(tokensA); inter.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA); union.addAll(tokensB);
        return (double) inter.size() / (double) union.size();
    }

    private static Set<String> normalizeToTokens(String text) {
        if (text == null) return Set.of();
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9\\s]", " ");
        Set<String> tokens = new HashSet<>();
        for (String tok : normalized.split("\\s+")) {
            if (tok == null) continue;
            tok = tok.trim();
            if (tok.length() >= 3 && !STOPWORDS.contains(tok)) tokens.add(tok);
        }
        return tokens;
    }
}
