package ctn.informatica.sca.integration.gema.dto;

import java.time.LocalDate;

public record UpdateTareaRequest(String titulo, LocalDate fecha, LocalDate fechaInicio, LocalDate fechaLimite,
        int total, int instrumentoId) {
}
