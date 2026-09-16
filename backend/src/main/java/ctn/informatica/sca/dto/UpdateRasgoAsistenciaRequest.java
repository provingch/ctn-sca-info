package ctn.informatica.sca.dto;

import java.util.List;

public record UpdateRasgoAsistenciaRequest(
        Integer asistenciaId,
        String estado,
        List<String> codigos
) {
}
