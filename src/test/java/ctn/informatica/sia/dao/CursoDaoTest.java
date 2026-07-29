package ctn.informatica.sia.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class CursoDaoTest {

    @Test
    public void shouldIncludeCourseWhenTeacherSpecialtyMatchesEvenWithoutDirectPlanilla() {
        Set<Integer> planillaCourseIds = Set.of(10);
        Set<Integer> teacherEspecialidadIds = Set.of(5);

        assertTrue(CursoDao.shouldIncludeCurso(10, 5, planillaCourseIds, teacherEspecialidadIds));
        assertTrue(CursoDao.shouldIncludeCurso(12, 5, planillaCourseIds, teacherEspecialidadIds));
        assertFalse(CursoDao.shouldIncludeCurso(13, 6, planillaCourseIds, teacherEspecialidadIds));
    }
}
