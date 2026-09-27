package ctn.informatica.sca.integration.gema.dto;

/** {@code ci} puede ser {@code null} (el DAO lo permite): GEMA debe tolerarlo. */
public record AlumnoDto(int id, String ci, String nombre, String apellido, int cursoId, CursoRefDto curso) {
}
