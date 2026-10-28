package ctn.informatica.sca.dto;

public record CargaHorariaSlotDto(
        int id,
        int diaSemana,
        int horaCatedraId,
        Integer horaCatedraNumero,
        String horaInicio,
        String horaFin,
        String materiaNombre,
        boolean comun,
        Integer especialidadId,
        String especialidadNombre,
        Integer cursoNivel,
        String cursoSeccion,
        String salaNombre
) {
}
