package ctn.informatica.sca.dto;

import java.util.List;

public record RasgoAsistenciaDto(
        int id,
        int alumnoId,
        String alumnoNombreCompleto,
        String estado,
        String faltaCodigo,
        String faltaObservacion,
        List<String> codigos
) {
}
