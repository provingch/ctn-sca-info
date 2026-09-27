package ctn.informatica.sca.dto;

import java.util.List;

/**
 * Alumno que supera el umbral de tareas no entregadas y/o de notas conductuales, sumando
 * todas las materias en las que el profesor autenticado lo tiene a cargo.
 */
public record AlumnoRiesgoDto(
        int alumnoId,
        String nombreCompleto,
        int cursoId,
        String cursoNombre,
        int especialidadId,
        String especialidadNombre,
        String seccion,
        int tareasNoEntregadas,
        int notasConductuales,
        List<String> motivos,
        List<AlumnoRiesgoMateriaDto> desglose
) {
}
