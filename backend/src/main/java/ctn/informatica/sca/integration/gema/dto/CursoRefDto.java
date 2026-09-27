package ctn.informatica.sca.integration.gema.dto;

/** Referencia embebida a un curso (identidad natural: especialidad+promoción+sección). */
public record CursoRefDto(String especialidad, int promocion, String seccion) {
}
