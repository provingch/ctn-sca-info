package ctn.informatica.sca.dto;

public record ProfileOwnerDto(
        Integer id,
        String nombre,
        String apellido,
        String fullName,
        Integer ci,
        String correo,
        String telefono,
        String celular,
        String usuario,
        Integer especialidadId,
        String googleEmail,
        String gcAccessToken,
        String firmaImagen,
        String fotoPerfil
) {
}
