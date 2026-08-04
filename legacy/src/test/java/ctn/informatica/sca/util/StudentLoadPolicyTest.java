package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StudentLoadPolicyTest {

    @Test
    void returnsExpectedStatusForCapacityValues() {
        assertEquals("ideal", StudentLoadPolicy.getCapacityStatus(28, 28));
        assertEquals("warning", StudentLoadPolicy.getCapacityStatus(26, 28));
        assertEquals("over", StudentLoadPolicy.getCapacityStatus(31, 28));
    }

    @Test
    void returnsHumanFriendlyMessages() {
        assertEquals("Curso dentro de la meta de 28 alumnos.", StudentLoadPolicy.getCapacityMessage(28, 28));
        assertEquals("Curso por debajo de la meta de 28 alumnos (falta 2).", StudentLoadPolicy.getCapacityMessage(26, 28));
        assertEquals("Curso por encima de la meta de 28 alumnos (excede 3).", StudentLoadPolicy.getCapacityMessage(31, 28));
    }
}
