package ctn.informatica.sca.dto;

public record PlanillaDto(
        int id,
        String nombre,
        int periodo,
        int tareasCount,
        int materiaId,
        int etapaSugerida
) {
}
