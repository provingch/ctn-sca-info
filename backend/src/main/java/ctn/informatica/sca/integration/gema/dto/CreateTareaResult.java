package ctn.informatica.sca.integration.gema.dto;

/** {@code created}: true (201) si es una tarea nueva, false (200) si fue un reenvío idempotente. */
public record CreateTareaResult(TareaDto tarea, boolean created) {
}
