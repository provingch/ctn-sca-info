package ctn.informatica.sca.dto;

public record AsignacionDto(
        Integer id,
        String materiaNombre,
        String cursoDescripcion,
        String especialidad,
        Integer cursoNivel,
        String cursoSeccion,
        Integer especialidadId,
        String cursoOrdinal,
        String estadoPlan
) {
}
