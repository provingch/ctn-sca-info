package ctn.informatica.sca.integration.gema.dto;

/** {@code etapa} usa los mismos valores que SCA internamente ({@code "primera"}/{@code "segunda"}). */
public record PlanillaDto(int id, int materiaId, int cursoId, int periodo, String etapa, Integer docenteCi,
        boolean etapa1Confirmada, boolean etapa2Confirmada) {
}
