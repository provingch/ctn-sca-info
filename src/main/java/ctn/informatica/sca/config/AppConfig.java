package ctn.informatica.sca.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;

public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static Properties props;

    public static synchronized void init(ServletContext context) {
        if (props != null) {
            return; // ya cargado, evita recargar en cada request
        }

        props = new Properties();
        boolean loaded = loadProperties(context, "/WEB-INF/config.properties");
        if (!loaded) {
            LOGGER.warning("No se encontro /WEB-INF/config.properties. Se intentara usar /WEB-INF/config.properties.example.");
            loaded = loadProperties(context, "/WEB-INF/config.properties.example");
        }

        if (!loaded) {
            LOGGER.warning("No se pudo cargar ninguna configuracion WEB-INF. La app iniciara con propiedades vacias.");
        }
    }

    private static boolean loadProperties(ServletContext context, String path) {
        try (InputStream is = context.getResourceAsStream(path)) {
            if (is == null) {
                return false;
            }
            props.load(is);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cargando " + path, e);
            return false;
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