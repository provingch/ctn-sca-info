package ctn.informatica.sca.integration.gema.dto;

/** Respuesta de {@code GET /api/integracion/gema/ping}. {@code database} es "ok" u "error". */
public record PingResponse(
        String status,
        String service,
        String client,
        String serverTime,
        String database,
        String version
) {
}
