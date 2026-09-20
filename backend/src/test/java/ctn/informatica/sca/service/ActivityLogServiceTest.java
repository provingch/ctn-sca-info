package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.UserDao;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ActivityLogServiceTest {

    @TempDir
    Path directorio;

    private ActivityLogService service;

    @BeforeEach
    void setUp() throws Exception {
        UserDao userDao = mock(UserDao.class);
        when(userDao.findActivityLogPathById(7)).thenReturn("usuario-7.txt");
        service = new ActivityLogService(userDao);
        ReflectionTestUtils.setField(service, "baseDir", directorio.toString());
    }

    @AfterEach
    void limpiarRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private List<String> lineas() throws Exception {
        return Files.readAllLines(directorio.resolve("usuario-7.txt"), StandardCharsets.UTF_8);
    }

    @Test
    void laLineaIncluyeLaIpEntreLaFechaYLaAccion() throws Exception {
        service.registrar(7, "Aprobó plan curricular", "203.0.113.9");

        List<String> lineas = lineas();
        assertEquals(1, lineas.size());
        assertTrue(lineas.get(0).matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] \\(ip: 203\\.0\\.113\\.9\\) Aprobó plan curricular"), lineas.get(0));
    }

    @Test
    void sinIpQuedaDesconocida() throws Exception {
        service.registrar(7, "Acción uno", null);
        service.registrar(7, "Acción dos", "  ");

        assertTrue(lineas().get(0).contains("(ip: desconocida) Acción uno"));
        assertTrue(lineas().get(1).contains("(ip: desconocida) Acción dos"));
    }

    @Test
    void dentroDeUnaRequestTomaLaIpRealDelClienteDetrasDeNginx() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(); // conexión desde 127.0.0.1, como nginx
        request.addHeader("X-Real-IP", "203.0.113.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(7, "Registró clase");

        assertTrue(lineas().get(0).contains("(ip: 203.0.113.9) Registró clase"), lineas().get(0));
        assertFalse(lineas().get(0).contains("127.0.0.1"), "no debe quedar la IP interna del proxy");
    }

    @Test
    void fueraDeUnaRequestQuedaDesconocida() throws Exception {
        service.registrar(7, "Tarea programada");

        assertTrue(lineas().get(0).contains("(ip: desconocida) Tarea programada"));
    }

    @Test
    void sinUsuarioONiAccionNoEscribeNada() throws Exception {
        service.registrar(0, "Algo", "1.2.3.4");
        service.registrar(7, "   ", "1.2.3.4");

        assertFalse(Files.exists(directorio.resolve("usuario-7.txt")));
    }

    @Test
    void leerUltimasDevuelveLasLineasTalCualIncluidasLasViejasSinIp() throws Exception {
        Files.writeString(directorio.resolve("usuario-7.txt"), "[2026-01-01 10:00:00] Inició sesión\n", StandardCharsets.UTF_8);
        service.registrar(7, "Cambió su contraseña", "203.0.113.9");

        List<String> ultimas = service.leerUltimas(7, 10);

        assertEquals("[2026-01-01 10:00:00] Inició sesión", ultimas.get(0));
        assertTrue(ultimas.get(1).contains("(ip: 203.0.113.9) Cambió su contraseña"));
    }
}
