package ctn.informatica.sca.dto;

public record HorarioResumenCursoDto(
        int cursoId,
        String especialidad,
        String cursoDescripcion,
        int cantidadSlotsCargados,
        int nivel
) {
}
