package ctn.informatica.sca.dto;

public record CreateHorarioSlotRequest(
        Integer asignacionId,
        Integer diaSemana,
        Integer horaCatedraId,
        Integer salaId
) {
}
