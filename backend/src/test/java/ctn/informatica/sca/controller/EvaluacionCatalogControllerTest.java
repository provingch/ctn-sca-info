package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class EvaluacionCatalogControllerTest {

    private CursoDao cursoDao;
    private EspecialidadDao especialidadDao;
    private EvaluacionCatalogController controller;
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(14L, null, List.of());

    @BeforeEach
    void setUp() {
        cursoDao = mock(CursoDao.class);
        especialidadDao = mock(EspecialidadDao.class);
        controller = new EvaluacionCatalogController(
                cursoDao,
                especialidadDao,
                mock(InstrumentoDao.class),
                mock(RasgoPlanillaDao.class));
    }

    @Test
    void evaluatorCatalogUsesEveryRegisteredCourse() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);

        List<EvaluacionCatalogController.CursoEvaluacionDto> result = controller.listCursos(null, authentication);

        assertEquals(2, result.size());
        verify(cursoDao).findAll();
    }

    @Test
    void specialtyFilterKeepsOnlyItsAvailableSections() throws Exception {
        ArrayList<Curso> cursos = new ArrayList<>(List.of(
                new Curso(1, "Informática", 2027, "A"),
                new Curso(2, "Informática", 2027, "B"),
                new Curso(3, "Construcciones Civiles", 2027, "C")));
        when(cursoDao.findAll()).thenReturn(cursos);
        when(especialidadDao.findById(5)).thenReturn(new Especialidad(5, "Informática"));

        List<EvaluacionCatalogController.CursoEvaluacionDto> result = controller.listCursos(5, authentication);

        assertEquals(List.of("A", "B"), result.stream().map(EvaluacionCatalogController.CursoEvaluacionDto::seccion).toList());
    }
}
