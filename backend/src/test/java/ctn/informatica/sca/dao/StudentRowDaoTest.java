package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StudentRowDaoTest {

    @Test
    void sumRelevantGrades_ignoresGradesOutsideTheCurrentTareaMaxSet() {
        Map<Integer, Integer> tareaMax = new HashMap<>();
        tareaMax.put(101, 5);
        tareaMax.put(202, 7);

        StudentRow row = new StudentRow();
        row.getGrades().put(101, 0);
        row.getGrades().put(202, 0);

        StudentRowDao.addGradeIfRelevant(row, 101, 5, tareaMax);
        StudentRowDao.addGradeIfRelevant(row, 202, 7, tareaMax);
        StudentRowDao.addGradeIfRelevant(row, 303, 99, tareaMax);

        assertFalse(row.getGrades().containsKey(303), "Un puntaje fuera del conjunto actual no debe registrarse");

        int total = StudentRowDao.sumRelevantGrades(row.getGrades(), tareaMax);
        assertEquals(12, total, "El total debe sumar solo las tareas vigentes de la etapa actual");

        Planilla planilla = new Planilla();
        planilla.setCategoria("comun");
        planilla.computeGradeRanges(12);

        row.setTotal(total);
        row.setNota(planilla.getNotaForSum(total));

        assertEquals(12, row.getTotal(), "StudentRow.total debe ignorar tareas ajenas a tareaMax");
        assertEquals(5, row.getNota(), "StudentRow.nota debe calcularse solo con la etapa actual");
    }
}
