package ctn.informatica.sca.web;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.model.Asignacion;

public class PlanCurricularControllerTest {

    @Test
    public void autodeteccion_debeElegirElOrdinalCorrecto() {
        PlanCurricularDto dto = new PlanCurricularDto();
        dto.disciplina = "Inglés";
        dto.especialidad = "Informática";
        dto.seccion = "A";
        dto.curso = "1º";

        Asignacion primero = asignacion(10, "1º");
        Asignacion segundo = asignacion(11, "2º");

        List<Asignacion> result = PlanCurricularController.findMatchingAssignments(dto, List.of(primero, segundo));

        assertEquals(List.of(primero), result);
    }

    private Asignacion asignacion(int id, String ordinal) {
        Asignacion asignacion = new Asignacion();
        asignacion.setId(id);
        asignacion.setMateriaNombre("Inglés");
        asignacion.setEspecialidad("Informática");
        asignacion.setCursoSeccion("A");
        asignacion.setCursoOrdinal(ordinal);
        return asignacion;
    }
}