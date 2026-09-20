package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.service.PlanillaReaperturaService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Los endpoints de /api/evaluacion son sólo de evaluación (nivel 2). SecurityConfig ya lo exige por ruta; esto
 * verifica la segunda barrera, la de método, evaluando de verdad @PreAuthorize con un contexto Spring mínimo.
 */
class EvaluacionMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        EvaluacionPlanillaController evaluacionPlanillaController() {
            return new EvaluacionPlanillaController(mock(PlanillaDao.class), mock(CursoDao.class), mock(EspecialidadDao.class),
                    mock(ProfesorDao.class), mock(PlanillaReaperturaService.class));
        }

        @Bean
        EvaluacionExportController evaluacionExportController() {
            return new EvaluacionExportController();
        }
    }

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private static UsernamePasswordAuthenticationToken usuario(int nivel) {
        return new UsernamePasswordAuthenticationToken((long) nivel, null, List.of(new SimpleGrantedAuthority("ROLE_LEVEL_" + nivel)));
    }

    @Test
    void ambosControllersDeclaranSoloNivel2() {
        assertEquals("hasRole('LEVEL_2')", EvaluacionPlanillaController.class.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('LEVEL_2')", EvaluacionExportController.class.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void profesorPadreYCoordinacionNoPuedenListarNiReabrirNiVerPlanillas() {
        EvaluacionPlanillaController controller = context.getBean(EvaluacionPlanillaController.class);
        for (int nivel : new int[] { 1, 4, 5 }) {
            UsernamePasswordAuthenticationToken auth = usuario(nivel);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThrows(AccessDeniedException.class, () -> controller.listarPlanillas(1, "primera", 2026, null, auth), "listar, nivel " + nivel);
            assertThrows(AccessDeniedException.class, () -> controller.verPlanilla(1, auth), "ver, nivel " + nivel);
            assertThrows(AccessDeniedException.class, () -> controller.reabrirEtapa1(1, null, auth), "reabrir 1, nivel " + nivel);
            assertThrows(AccessDeniedException.class, () -> controller.reabrirEtapa2(1, null, auth), "reabrir 2, nivel " + nivel);
        }
    }

    @Test
    void profesorYPadreNoPuedenDescargarElExcel() {
        EvaluacionExportController controller = context.getBean(EvaluacionExportController.class);
        for (int nivel : new int[] { 1, 4 }) {
            UsernamePasswordAuthenticationToken auth = usuario(nivel);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThrows(AccessDeniedException.class, () -> controller.export(1, "primera", 2026, null, auth, new MockHttpServletResponse()), "export, nivel " + nivel);
        }
    }

    @Test
    void evaluacionSiPasaLaBarreraDeMetodo() {
        EvaluacionPlanillaController controller = context.getBean(EvaluacionPlanillaController.class);
        UsernamePasswordAuthenticationToken auth = usuario(2);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // con DAOs mockeados el curso no existe: llega al cuerpo del método (404), no se corta antes con 403
        Throwable ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.listarPlanillas(1, "primera", 2026, null, auth));
        assertEquals(404, ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value());
        assertDoesNotThrow(() -> SecurityContextHolder.getContext().getAuthentication());
    }
}
