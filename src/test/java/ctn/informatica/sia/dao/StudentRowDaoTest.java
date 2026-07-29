package ctn.informatica.sia.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StudentRowDaoTest {

    @Test
    public void shouldAttachAlumnoOnlyWhenItBelongsToThePlanillaCourse() {
        assertTrue(StudentRowDao.shouldIncludeAlumnoForPlanillaCurso(10, 10));
        assertFalse(StudentRowDao.shouldIncludeAlumnoForPlanillaCurso(10, 11));
    }

    @Test
    public void shouldDefaultMissingGradeValuesToZero() {
        assertEquals(0, StudentRowDao.normalizeGradeValue(null));
        assertEquals(5, StudentRowDao.normalizeGradeValue(5));
    }
}
