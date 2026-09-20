package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class IncumplimientoControllerTest {

    private IncumplimientoRevisionDao dao;
    private IncumplimientoController controller;
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(22L, null, List.of());

    @BeforeEach
    void setUp() {
        dao = mock(IncumplimientoRevisionDao.class);
        controller = new IncumplimientoController(dao);
    }

    @Test
    void misIncongruenciasListaSoloLasDelUsuarioAutenticado() throws Exception {
        when(dao.listarIncongruenciasPendientesPorUsuario(22)).thenReturn(List.of(Map.of("id", 3L)));

        assertEquals(1, controller.misIncongruencias(authentication).size());
        verify(dao).listarIncongruenciasPendientesPorUsuario(22);
    }

    @Test
    void justificarGuardaElTextoRecortadoParaElUsuarioAutenticado() throws Exception {
        when(dao.justificar(3, 22, "Estaba de licencia")).thenReturn(true);

        Map<String, Object> result = controller.justificar(3, Map.of("justificacion", "  Estaba de licencia  "), authentication);

        assertEquals(true, result.get("ok"));
        verify(dao).justificar(3, 22, "Estaba de licencia");
    }

    @Test
    void justificarVaciaOMuyLargaEsError400() {
        for (String texto : new String[] {"", "   ", "x".repeat(IncumplimientoController.JUSTIFICACION_MAX_LEN + 1)}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> controller.justificar(3, Map.of("justificacion", texto), authentication));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
        verifyNoInteractions(dao);
    }

    @Test
    void justificarUnaIncongruenciaAjenaOYaResueltaEs404() throws Exception {
        when(dao.justificar(3, 22, "texto")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.justificar(3, Map.of("justificacion", "texto"), authentication));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
