package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.integration.gema.dto.CursoDto;
import ctn.informatica.sca.model.Curso;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GemaCursoServiceTest {

    @Test
    void listCursosMapsAllWhenNoFiltro() throws SQLException {
        CursoDao dao = mock(CursoDao.class);
        when(dao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Electrónica", 2028, "B"))));
        GemaCursoService service = new GemaCursoService(dao);

        List<CursoDto> result = service.listCursos(null);

        assertEquals(2, result.size());
        assertEquals(new CursoDto(1, "Informática", 2027, "A"), result.get(0));
    }

    @Test
    void listCursosFiltraPorEspecialidadIgnorandoMayusculas() throws SQLException {
        CursoDao dao = mock(CursoDao.class);
        when(dao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Electrónica", 2028, "B"))));
        GemaCursoService service = new GemaCursoService(dao);

        List<CursoDto> result = service.listCursos("informática");

        assertEquals(1, result.size());
        assertEquals("Informática", result.get(0).especialidad());
    }

    @Test
    void sqlExceptionSeConvierteEn503() throws SQLException {
        CursoDao dao = mock(CursoDao.class);
        when(dao.findAll()).thenThrow(new SQLException("caída"));
        GemaCursoService service = new GemaCursoService(dao);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.listCursos(null));
        assertEquals(503, ex.getStatusCode().value());
    }
}
