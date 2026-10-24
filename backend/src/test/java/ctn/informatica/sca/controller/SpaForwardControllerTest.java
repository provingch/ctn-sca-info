package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class SpaForwardControllerTest {

    /** Un F5 sobre estas páginas públicas tiene que volver a index.html, no dar 401/404. */
    @Test
    void reenviaALaSpaLasPaginasPublicas() throws Exception {
        List<String> rutas = Arrays.asList(
                SpaForwardController.class.getMethod("forwardToIndex").getAnnotation(GetMapping.class).value());

        for (String publica : List.of("/login", "/privacidad", "/terminos", "/acerca-de")) {
            assertTrue(rutas.contains(publica), "Falta reenviar " + publica + " a index.html");
        }
    }
}
