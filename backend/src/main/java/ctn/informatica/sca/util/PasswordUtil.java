package ctn.informatica.sca.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordUtil() {
    }

    /**
     * Genera un hash seguro de la contraseña usando BCrypt.
     */
    public static String hash(String plainText) {
        if (plainText == null) {
            plainText = "password";
        }
        return ENCODER.encode(plainText);
    }

    /**
     * Verifica si una contraseña en texto plano coincide con el hash almacenado.
     */
    public static boolean matches(String plainText, String stored) {
        if (stored == null || plainText == null) {
            return false;
        }
        return isBcryptHash(stored) && ENCODER.matches(plainText, stored);
    }

    /**
     * Distingue un hash BCrypt real de una contraseña vieja en texto plano.
     */
    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
