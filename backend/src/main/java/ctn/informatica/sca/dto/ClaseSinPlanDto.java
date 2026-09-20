package ctn.informatica.sca.dto;

import java.time.LocalDate;

/** Clase (planilla_rasgo) dada sin plan aprobado, candidata a la comparación retroactiva. */
public record ClaseSinPlanDto(int id, int usuarioId, String tema, LocalDate fechaClase) {
}
