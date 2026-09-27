package ctn.informatica.sca.integration.gema.dto;

/** Nunca copia contraseña, TOTP ni tokens de Google — ver {@code ProfesorDao}/{@code Profesor}. */
public record DocenteDto(int id, Integer ci, String nombre, String apellido, int nivel) {
}
