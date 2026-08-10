package ctn.informatica.sca.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    /**
     * Genera un hash seguro de la contraseña usando BCrypt.
     */
    public static String hash(String plainText) {
        if (plainText == null) {
            plainText = "password";
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con el hash almacenado.
     * Soporta tanto hashes BCrypt como texto plano (para migración transparente).
     */
    public static boolean matches(String plainText, String stored) {
        if (stored == null || plainText == null) {
            return false;
        }
        if (isBcryptHash(stored)) {
            try {
                return BCrypt.checkpw(plainText, stored);
            } catch (IllegalArgumentException ex) {
                // Algunos salts históricos o revisiones ($2b$) pueden no ser
                // aceptadas por la versión de jBCrypt incluida en el runtime.
                // Intentamos un fallback a la implementación de Spring
                // `BCryptPasswordEncoder` que soporta más revisiones.
                try {
                    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    return encoder.matches(plainText, stored);
                } catch (Exception ex2) {
                    // Si tampoco funciona, no hay forma de validar con este hash.
                    return false;
                }
            }
        }
        // Fallback para migración: comparación de texto plano (será rehasheado en el próximo login)
        return stored.equals(plainText);
    }

    /**
     * Distingue un hash BCrypt real de una contraseña vieja en texto plano.
     */
    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private static String normalizeBcryptPrefix(String hash) {
        if (hash == null) return null;
        if (hash.startsWith("$2a$") || hash.startsWith("$2y$") || hash.startsWith("$2x$")) {
            return "$2b$" + hash.substring(4);
        }
        return hash;
    }
}
