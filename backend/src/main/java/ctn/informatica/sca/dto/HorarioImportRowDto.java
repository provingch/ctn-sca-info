package ctn.informatica.sca.dto;

public record HorarioImportRowDto(
        int diaSemana,
        int horaCatedraId,
        String horaCatedraEtiqueta,
        String materiaTexto,
        String profesorTexto,
        Integer asignacionId,
        String estado,
        String detalle) {
}
