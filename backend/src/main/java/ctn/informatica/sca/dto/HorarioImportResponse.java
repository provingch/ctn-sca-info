package ctn.informatica.sca.dto;

import java.util.List;

public record HorarioImportResponse(int creados, int omitidos, List<HorarioImportRowDto> filas) {
}
