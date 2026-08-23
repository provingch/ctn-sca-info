package ctn.informatica.sca.dto;

public record HoraCatedraDto(
        int id,
        int numero,
        String etiqueta,
        String horaInicio,
        String horaFin
) {
}
