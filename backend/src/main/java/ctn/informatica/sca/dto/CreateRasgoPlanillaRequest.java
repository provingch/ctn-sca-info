package ctn.informatica.sca.dto;

import java.util.List;

public record CreateRasgoPlanillaRequest(
        Integer cursoId,
        Integer etapa,
        Integer instrumentoId,
        String turno,
        String tema,
        List<Integer> alumnosAusentes
) {
}
