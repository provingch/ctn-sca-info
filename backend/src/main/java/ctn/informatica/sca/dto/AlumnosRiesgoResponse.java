package ctn.informatica.sca.dto;

import java.util.List;

public record AlumnosRiesgoResponse(
        int umbralTareas,
        int umbralConducta,
        List<AlumnoRiesgoDto> alumnos
) {
}
