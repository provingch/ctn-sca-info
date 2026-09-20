package ctn.informatica.sca.util;

public final class NombreUtil {
    private NombreUtil() {}

    /** "nombre apellido", en ese orden, con nulls/blancos tolerados. */
    public static String completo(String nombre, String apellido) {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellido == null ? "" : apellido.trim();
        return (n + (a.isEmpty() ? "" : " " + a)).trim();
    }

    /** Primera palabra de nombre + primera palabra de apellido. Para pantallas, no para documentos. */
    public static String corto(String nombre, String apellido) {
        String n = primeraPalabra(nombre);
        String a = primeraPalabra(apellido);
        return (n + (a.isEmpty() ? "" : " " + a)).trim();
    }

    private static String primeraPalabra(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        int sp = t.indexOf(' ');
        return sp < 0 ? t : t.substring(0, sp);
    }
}
