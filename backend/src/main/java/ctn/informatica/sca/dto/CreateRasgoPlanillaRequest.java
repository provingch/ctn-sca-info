package ctn.informatica.sca.dto;

import java.util.List;
import java.util.Map;

public record CreateRasgoPlanillaRequest(
        Integer cursoId,
        Integer etapa,
        Integer instrumentoId,
        String turno,
        String tema,
        List<Integer> alumnosAusentes,
        Map<Integer, List<String>> codigosPorAlumno
) {
}
