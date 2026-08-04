package ctn.informatica.sca.config;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.io.InputStream;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void getUsesSystemPropertiesWhenNoConfigFileIsPresent() throws Exception {
        clearCachedProperties();
        System.setProperty("google.client.id", "from-system-id");
        System.setProperty("google.client.secret", "from-system-secret");
        System.setProperty("google.redirect.uri", "https://example.test/callback");

        ServletContext context = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("getResourceAsStream".equals(method.getName())) {
                        return null;
                    }
                    if ("getAttribute".equals(method.getName()) || "setAttribute".equals(method.getName())) {
                        return null;
                    }
                    if ("getContextPath".equals(method.getName())) {
                        return "";
                    }
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );

        AppConfig.init(context);

        assertEquals("from-system-id", AppConfig.get("google.client.id"));
        assertEquals("from-system-secret", AppConfig.get("google.client.secret"));
        assertEquals("https://example.test/callback", AppConfig.get("google.redirect.uri"));
    }

    private static void clearCachedProperties() throws Exception {
        Field field = AppConfig.class.getDeclaredField("props");
        field.setAccessible(true);
        field.set(null, null);
    }
}
