package ctn.informatica.sca.dto;

public record AssignFaltaCodigoRequest(
        Integer asistenciaId,
        String faltaCodigo,
        String faltaObservacion
) {
}
