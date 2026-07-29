package ctn.informatica.sia.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class PlanillaDaoTest {

    @Test
    public void shouldAllowOnlyTeacherLinkedMaterias() {
        Set<Integer> allowedMateriaIds = Set.of(4, 7);

        assertTrue(PlanillaDao.shouldIncludePlanillaForProfesor(4, allowedMateriaIds));
        assertTrue(PlanillaDao.shouldIncludePlanillaForProfesor(7, allowedMateriaIds));
        assertFalse(PlanillaDao.shouldIncludePlanillaForProfesor(9, allowedMateriaIds));
    }
}
