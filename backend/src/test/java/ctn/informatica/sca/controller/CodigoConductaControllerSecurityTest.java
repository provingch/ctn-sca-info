package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ctn.informatica.sca.controller.CodigoConductaController.CodigoConductaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alta y baja de códigos de conducta: exclusivas de Coordinación Pedagógica (nivel 5).
 * El listado queda abierto a cualquier usuario autenticado porque lo usa el editor de rasgos.
 */
class CodigoConductaControllerSecurityTest {

    @Test
    void crear_soloCoordinacionPedagogica() throws Exception {
        PreAuthorize rule = CodigoConductaController.class
                .getMethod("crear", CodigoConductaRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('LEVEL_5')", rule.value());
    }

    @Test
    void desactivar_soloCoordinacionPedagogica() throws Exception {
        PreAuthorize rule = CodigoConductaController.class
                .getMethod("desactivar", int.class)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('LEVEL_5')", rule.value());
    }

    @Test
    void listar_quedaAbiertoAUsuariosAutenticados() throws Exception {
        assertNull(CodigoConductaController.class.getMethod("listar").getAnnotation(PreAuthorize.class));
    }
}
