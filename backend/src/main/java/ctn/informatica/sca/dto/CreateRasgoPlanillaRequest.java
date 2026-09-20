package ctn.informatica.sca.dto;

import java.util.List;
import java.util.Map;

public record CreateRasgoPlanillaRequest(
        Integer cursoId,
        Integer asignacionId,
        Integer instrumentoId,
        String horaInicio,
        Integer horasCatedra,
        String modalidad,
        String observaciones,
        String tema,
        String justificacionAtraso,
        List<Integer> alumnosAusentes,
        Map<Integer, List<String>> codigosPorAlumno
) {
}
