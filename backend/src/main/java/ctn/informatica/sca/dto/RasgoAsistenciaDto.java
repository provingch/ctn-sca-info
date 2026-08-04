package ctn.informatica.sca.dto;

public record RasgoAsistenciaDto(
        int id,
        String alumnoNombreCompleto,
        String estado,
        String faltaCodigo,
        String faltaObservacion
) {
}
