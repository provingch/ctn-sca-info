package ctn.informatica.sca.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);
    private static final SecureRandom RANDOM = new SecureRandom();
    // sin 0/O, 1/l/I: se dicta o se copia a mano
    private static final String ALFABETO_TEMPORAL = "abcdefghjkmnpqrstuvwxyz23456789";

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

    /** Contraseña temporal de 10 caracteres para un restablecimiento hecho por un administrador. */
    public static String temporal() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(ALFABETO_TEMPORAL.charAt(RANDOM.nextInt(ALFABETO_TEMPORAL.length())));
        return sb.toString();
    }

    /**
     * Distingue un hash BCrypt real de una contraseña vieja en texto plano.
     */
    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
