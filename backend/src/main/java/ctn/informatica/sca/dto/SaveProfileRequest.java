package ctn.informatica.sca.dto;

public record SaveProfileRequest(
        String correo,
        String telefono,
        String celular,
        String usuario,
        String nombre,
        String apellido,
        Integer ci,
        Integer nivel
) {
}
