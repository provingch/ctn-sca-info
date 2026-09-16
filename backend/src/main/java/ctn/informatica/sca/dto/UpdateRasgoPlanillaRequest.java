package ctn.informatica.sca.dto;

import java.util.List;

public record UpdateRasgoPlanillaRequest(
        String tema,
        List<UpdateRasgoAsistenciaRequest> asistencias
) {
}
