package ctn.informatica.sia.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.servlet.ServletContext;

public class AppConfig {

    private static Properties props;

    public static synchronized void init(ServletContext context) {
        if (props != null) {
            return; // ya cargado, evita recargar en cada request
        }

        props = new Properties();
        try (InputStream is = context.getResourceAsStream("/WEB-INF/config.properties")) {
            if (is == null) {
                throw new RuntimeException("No se encontró /WEB-INF/config.properties");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando config.properties", e);
        }
    }

    public static String get(String key) {
        if (props == null) {
            throw new IllegalStateException("AppConfig no fue inicializado. Llamá a AppConfig.init() primero.");
        }
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("No existe la clave '" + key + "' en config.properties");
        }
        return value;
    }
}