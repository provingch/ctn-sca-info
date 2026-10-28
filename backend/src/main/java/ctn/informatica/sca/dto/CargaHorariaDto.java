package ctn.informatica.sca.dto;

import java.util.List;

/** Horario semanal de un profesor; incluye el catálogo de horas para dibujar la grilla sin endpoints de admin. */
public record CargaHorariaDto(
        int usuarioId,
        String profesorNombre,
        List<HoraCatedraDto> catalogo,
        List<CargaHorariaSlotDto> slots
) {
}
