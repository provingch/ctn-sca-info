package ctn.informatica.sca.dto;

/**
 * Resumen de una clase dictada (planilla_rasgo) para las vistas "clases dadas"
 * del profesor y del admin de especialidad.
 */
public record ClaseDadaDto(
        int id,
        String fechaClase,
        String tema,
        int cursoId,
        String cursoDescripcion,
        Integer asignacionId,
        String materiaNombre,
        int profesorId,
        String profesorNombre,
        Integer especialidadId,
        String especialidadNombre,
        int totalAlumnos,
        int totalAusentes,
        int totalJustificados,
        String createdAt,
        int totalPresentes,
        int totalPendientes
) {
}
