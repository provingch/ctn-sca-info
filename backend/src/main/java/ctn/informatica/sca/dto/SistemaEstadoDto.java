package ctn.informatica.sca.dto;

import java.util.List;

public record SistemaEstadoDto(
        boolean dbConectada,
        List<MigracionDto> migraciones,
        String ultimaSyncClassroom,
        long espacioLogsBytes
) {
}
