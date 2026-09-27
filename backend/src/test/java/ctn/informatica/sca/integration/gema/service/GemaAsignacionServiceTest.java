package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.AsignacionDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GemaAsignacionServiceTest {

    private static Asignacion asignacion(int id, int profesorId, int materiaId, String especialidad, int nivel, String seccion) {
        Asignacion a = new Asignacion(id, profesorId, materiaId, 0);
        a.setMateriaNombre("Programación");
        a.setEspecialidad(especialidad);
        a.setCursoNivel(nivel);
        a.setCursoSeccion(seccion);
        return a;
    }

    private static Profesor profesor(int id, int ci) {
        Profesor p = new Profesor();
        p.setId(id);
        p.setCi(ci);
        p.setNombre("Ana");
        p.setApellido("Pérez");
        p.setNivel(1);
        return p;
    }

    @Test
    void mapeaPromocionConLaMismaFormulaQueCurso() throws SQLException {
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        when(asignacionDao.findAll()).thenReturn(List.of(asignacion(1, 10, 20, "Informática", 3, "A")));
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555)));
        GemaAsignacionService service = new GemaAsignacionService(asignacionDao, profesorDao, cursoDao);

        List<AsignacionDto> result = service.listAsignaciones(null, null);

        assertEquals(1, result.size());
        AsignacionDto dto = result.get(0);
        assertEquals(555, dto.docente().ci());
        assertEquals(AcademicPeriod.current() - 3 + 3, dto.curso().promocion());
    }

    @Test
    void filtraPorDocenteCiSinCoincidenciaDevuelveVacio() throws SQLException {
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555)));
        GemaAsignacionService service = new GemaAsignacionService(asignacionDao, profesorDao, cursoDao);

        List<AsignacionDto> result = service.listAsignaciones(999, null);

        assertEquals(0, result.size());
    }

    @Test
    void filtraPorCursoIdResuelto() throws SQLException {
        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        int nivel = 3;
        int promocionEsperada = AcademicPeriod.current() - nivel + 3;
        when(asignacionDao.findAll()).thenReturn(List.of(
                asignacion(1, 10, 20, "Informática", nivel, "A"),
                asignacion(2, 11, 21, "Electrónica", nivel, "B")));
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555), profesor(11, 777)));
        when(cursoDao.findById(5)).thenReturn(new Curso(5, "Informática", promocionEsperada, "A"));
        GemaAsignacionService service = new GemaAsignacionService(asignacionDao, profesorDao, cursoDao);

        List<AsignacionDto> result = service.listAsignaciones(null, 5);

        assertEquals(1, result.size());
        assertEquals("Informática", result.get(0).curso().especialidad());
    }
}
