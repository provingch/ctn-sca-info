package ctn.informatica.sca.dto;

/**
 * Un bloque de clase del profesor para hoy, resultado de agrupar hora_catedra
 * consecutivas de la misma asignación. {@code cursoId} es el id real de
 * `curso` (el mismo que espera create-rasgo-planilla), no el de curso_base.
 */
public record HorarioBloqueHoyDto(
        int asignacionId,
        int cursoId,
        String materiaNombre,
        String cursoDescripcion,
        String salaNombre,
        String horaInicio,
        String horaFin,
        int horasCatedra,
        boolean registrada
) {
}
