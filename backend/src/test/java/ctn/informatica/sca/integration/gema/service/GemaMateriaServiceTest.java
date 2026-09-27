package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.integration.gema.dto.MateriaDto;
import ctn.informatica.sca.model.Materia;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GemaMateriaServiceTest {

    @Test
    void listMateriasIncluyeEspecialidadIds() throws SQLException {
        MateriaDao dao = mock(MateriaDao.class);
        when(dao.listAll()).thenReturn(List.of(new Materia(1, "Matemática", "comun")));
        when(dao.listEspecialidadIdsForMateria(1)).thenReturn(List.of(10, 20));
        GemaMateriaService service = new GemaMateriaService(dao);

        List<MateriaDto> result = service.listMaterias();

        assertEquals(1, result.size());
        assertEquals(new MateriaDto(1, "Matemática", "comun", List.of(10, 20)), result.get(0));
    }
}
