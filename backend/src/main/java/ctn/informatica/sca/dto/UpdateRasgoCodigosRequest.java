package ctn.informatica.sca.dto;

import java.util.List;

public record UpdateRasgoCodigosRequest(
        Integer asistenciaId,
        List<String> codigos
) {
}