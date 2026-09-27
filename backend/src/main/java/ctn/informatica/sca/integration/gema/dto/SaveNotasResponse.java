package ctn.informatica.sca.integration.gema.dto;

import java.util.List;

/** Todo o nada: {@code guardadas} es 0 si {@code rechazadas} no está vacío. */
public record SaveNotasResponse(int recibidas, int guardadas, List<NotaRechazadaDto> rechazadas) {
}
