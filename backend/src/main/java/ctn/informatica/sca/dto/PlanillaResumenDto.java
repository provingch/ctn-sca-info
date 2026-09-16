package ctn.informatica.sca.dto;

/**
 * Item liviano para listar las planillas del profesor en varios cursos a la
 * vez (home sin cursoId seleccionado). No incluye la portada: solo el
 * booleano tienePortada, para no inflar la respuesta con imágenes.
 */
public record PlanillaResumenDto(
        int id,
        int materiaId,
        String materiaNombre,
        int cursoId,
        String cursoOrdinal,
        String seccion,
        String especialidadNombre,
        int etapa,
        boolean tienePortada
) {
}
