package ctn.informatica.sca.dto;

/**
 * Nota de conducta ("N") registrada por un profesor durante una clase.
 * Cada fila representa un código de conducta asignado a un alumno en una
 * planilla_rasgo puntual, para que los padres vean las notas conductuales de sus hijos.
 */
public record RasgoConductaDto(
        String fechaClase,
        String materia,
        String profesorNombre,
        String codigo,
        String descripcion,
        String observacion
) {
}
