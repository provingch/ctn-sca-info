package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.integration.gema.dto.AlumnoDto;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GemaAlumnoServiceTest {

    private static Alumno alumno(int id, String ci, int cursoId) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setCi(ci);
        a.setNombre("Juan");
        a.setApellido("Gómez");
        a.setCursoId(cursoId);
        return a;
    }

    @Test
    void listAlumnosActivosResuelveCursoRef() throws SQLException {
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(1, "1234567", 5)));
        when(cursoDao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(new Curso(5, "Informática", 2027, "A"))));
        GemaAlumnoService service = new GemaAlumnoService(alumnoDao, cursoDao);

        List<AlumnoDto> result = service.listAlumnos(null, null, false);

        assertEquals(1, result.size());
        assertEquals("Informática", result.get(0).curso().especialidad());
        assertEquals("A", result.get(0).curso().seccion());
    }

    @Test
    void ciNuloEsTolerado() throws SQLException {
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(1, null, 5)));
        when(cursoDao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(new Curso(5, "Informática", 2027, "A"))));
        GemaAlumnoService service = new GemaAlumnoService(alumnoDao, cursoDao);

        List<AlumnoDto> result = service.listAlumnos(null, null, false);

        assertNull(result.get(0).ci());
    }

    @Test
    void filtraPorCursoIdYCi() throws SQLException {
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(1, "111", 5), alumno(2, "222", 6)));
        when(cursoDao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(
                new Curso(5, "Informática", 2027, "A"), new Curso(6, "Electrónica", 2027, "B"))));
        GemaAlumnoService service = new GemaAlumnoService(alumnoDao, cursoDao);

        assertEquals(1, service.listAlumnos(5, null, false).size());
        assertEquals(1, service.listAlumnos(null, "222", false).size());
        assertEquals(0, service.listAlumnos(5, "222", false).size());
    }

    @Test
    void egresadosUsaOtraFuenteYSeccionQuedaNula() throws SQLException {
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        Alumno egresado = alumno(9, "999", 50);
        egresado.setEspecialidadNombre("Informática");
        egresado.setPromocion(2020);
        when(alumnoDao.findAllEgresados()).thenReturn(List.of(egresado));
        GemaAlumnoService service = new GemaAlumnoService(alumnoDao, cursoDao);

        List<AlumnoDto> result = service.listAlumnos(null, null, true);

        assertEquals(1, result.size());
        assertEquals(2020, result.get(0).curso().promocion());
        assertNull(result.get(0).curso().seccion());
    }
}
