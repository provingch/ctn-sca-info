package ctn.informatica.sca.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RasgoAsistenciaTest {

    @Test
    void shouldReturnFriendlyDescriptionForKnownCodes() {
        RasgoAsistencia asistencia = new RasgoAsistencia();
        asistencia.setFaltaCodigo("N1");

        assertEquals("Llegada tardía a clase", asistencia.getCodigoDescripcion());
    }

    @Test
    void shouldReturnN9Description() {
        RasgoAsistencia asistencia = new RasgoAsistencia();
        asistencia.setFaltaCodigo("N9");

        assertEquals("Ausente en clase, presente en la Institución", asistencia.getCodigoDescripcion());
    }
}
