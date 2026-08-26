package ctn.informatica.sca.util;

import org.mindrot.jbcrypt.BCrypt;

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
     */
    public static boolean matches(String plainText, String stored) {
        if (stored == null || plainText == null) {
            return false;
        }
        if (isBcryptHash(stored)) {
            try {
                return BCrypt.checkpw(plainText, stored);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return false;
    }

    /**
     * Distingue un hash BCrypt real de una contraseña vieja en texto plano.
     */
    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
