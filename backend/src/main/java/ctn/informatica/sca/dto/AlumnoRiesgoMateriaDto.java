package ctn.informatica.sca.dto;

/** Desglose por materia de un {@link AlumnoRiesgoDto}. */
public record AlumnoRiesgoMateriaDto(
        int materiaId,
        String materiaNombre,
        int tareasNoEntregadas,
        int notasConductuales
) {
}
