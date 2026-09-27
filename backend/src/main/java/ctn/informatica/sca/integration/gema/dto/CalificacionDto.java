package ctn.informatica.sca.integration.gema.dto;

/** {@code puntos == null}: sin entregar o sin calificar todavía (distinto de 0). */
public record CalificacionDto(String alumnoCi, int alumnoId, String gemaTareaId, Integer puntos) {
}
