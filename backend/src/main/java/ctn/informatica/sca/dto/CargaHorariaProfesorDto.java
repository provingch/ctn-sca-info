package ctn.informatica.sca.dto;

public record CargaHorariaProfesorDto(
        int usuarioId,
        String profesorNombre,
        int horas,
        int horasComun,
        int horasEspecificas,
        int minutos
) {
}
