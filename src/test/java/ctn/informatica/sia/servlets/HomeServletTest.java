package ctn.informatica.sia.servlets;

import ctn.informatica.sia.model.Planilla;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
