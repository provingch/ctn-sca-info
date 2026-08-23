package ctn.informatica.sca.dto;

public record HorarioSlotDto(
        int id,
        int asignacionId,
        int diaSemana,
        int horaCatedraId,
        int horaCatedraNumero,
        String horaCatedraEtiqueta,
        String horaInicio,
        String horaFin,
        String sala,
        String materiaNombre,
        String cursoDescripcion,
        String profesorNombre
) {
}
