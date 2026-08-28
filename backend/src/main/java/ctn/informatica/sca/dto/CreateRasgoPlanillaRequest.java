package ctn.informatica.sca.dto;

import java.util.List;
import java.util.Map;

public record CreateRasgoPlanillaRequest(
        Integer cursoId,
        Integer asignacionId,
        Integer etapa,
        Integer instrumentoId,
        String turno,
        String tema,
        String justificacionAtraso,
        List<Integer> alumnosAusentes,
        Map<Integer, List<String>> codigosPorAlumno
) {
}
