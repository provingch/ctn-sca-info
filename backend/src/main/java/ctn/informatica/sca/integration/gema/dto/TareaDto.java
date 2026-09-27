package ctn.informatica.sca.integration.gema.dto;

import java.time.LocalDate;

/** {@code origen}: {@code "CLASSROOM"} si viene de Google Classroom, {@code "GEMA"} si la creó esta integración, si no {@code "SCA"}. */
public record TareaDto(int id, String titulo, LocalDate fecha, LocalDate fechaInicio, LocalDate fechaLimite,
        int total, int instrumentoId, String instrumentoNombre, String origen) {
}
