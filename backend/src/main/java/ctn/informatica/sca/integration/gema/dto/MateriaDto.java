package ctn.informatica.sca.integration.gema.dto;

import java.util.List;

public record MateriaDto(int id, String nombre, String categoria, List<Integer> especialidadIds) {
}
