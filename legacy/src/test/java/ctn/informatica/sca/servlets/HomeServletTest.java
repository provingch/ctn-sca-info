package ctn.informatica.sca.servlets;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.User;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeServletTest {

    @Test
    void shouldRenderPlanillaCardsWhenGoogleClassroomIsNotConnectedButPlanillasExist() {
        HomeServlet servlet = new HomeServlet();
        List<Planilla> planillas = List.of(
                new Planilla(1, 10, 20, "Matemática", 2025, "primera", 3, 2, "2025-01-10")
        );

        assertTrue(servlet.shouldRenderPlanillaCards(planillas));
    }

    @Test
    void shouldDefaultToCompleteClassViewWhenNoExplicitViewIsProvided() throws Exception {
        HomeServlet servlet = new HomeServlet();
        Method method = HomeServlet.class.getDeclaredMethod("resolveViewMode", String.class, User.class);
        method.setAccessible(true);

        Object resolved = method.invoke(servlet, "", new User(1, "teacher", "Profesor", 1));

        assertEquals("clase", resolved);
    }
}
