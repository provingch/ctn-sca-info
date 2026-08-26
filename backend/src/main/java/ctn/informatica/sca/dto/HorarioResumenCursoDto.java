package ctn.informatica.sca.dto;

public record HorarioResumenCursoDto(
        int cursoId,
        int especialidadId,
        String especialidad,
        String cursoDescripcion,
        int cantidadSlotsCargados,
        int nivel
) {
}
