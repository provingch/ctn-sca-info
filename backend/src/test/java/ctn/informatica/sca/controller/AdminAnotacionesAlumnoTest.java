package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.AnotacionWebDao;
import ctn.informatica.sca.model.Alumno;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/** Ficha del alumno en Admin: anotaciones sin imagen y la foto de evidencia solo al pedirla, del mismo alumno. */
class AdminAnotacionesAlumnoTest {

    private static final Authentication ADMIN = new UsernamePasswordAuthenticationToken(1L, null, List.of());

    private final AlumnoDao alumnoDao = mock(AlumnoDao.class);
    private final AnotacionWebDao anotacionDao = mock(AnotacionWebDao.class);
    private final AdminController controller = new AdminController(null, null, null, null, null, alumnoDao) {
        @Override protected Integer getSpecialtyAdminIdForUser(int userId) { return null; }
        @Override protected AnotacionWebDao anotacionWebDao() { return anotacionDao; }
    };

    private static Alumno alumno(int id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setCursoId(3);
        return alumno;
    }

    @Test
    void listaLasAnotacionesDelAlumno() throws Exception {
        when(alumnoDao.findById(5)).thenReturn(alumno(5));
        when(anotacionDao.porAlumno(5)).thenReturn(List.of(new AnotacionWebDao.Anotacion(9, 5, "Ana", "Gómez", "Informática", null, "B",
                LocalDateTime.of(2026, 10, 21, 7, 30), "No trae corbata", null, null, null, null, 4)));
        List<AnotacionWebController.AnotacionDto> lista = controller.anotacionesAlumno(5, ADMIN);
        assertEquals(1, lista.size());
        assertEquals(true, lista.get(0).tieneFoto());
    }

    @Test
    void alumnoInexistenteDa404() throws Exception {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.anotacionesAlumno(77, ADMIN));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(anotacionDao, never()).porAlumno(77);
    }

    @Test
    void laFotoSeFiltraPorAlumno() throws Exception {
        when(alumnoDao.findById(5)).thenReturn(alumno(5));
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 1};
        when(anotacionDao.foto(9, 5)).thenReturn(new AnotacionWebDao.Foto("image/jpeg", jpeg));
        assertArrayEquals(jpeg, controller.fotoAnotacionAlumno(5, 9, ADMIN).getBody());

        ResponseStatusException ajena = assertThrows(ResponseStatusException.class, () -> controller.fotoAnotacionAlumno(5, 10, ADMIN));
        assertEquals(HttpStatus.NOT_FOUND, ajena.getStatusCode());
    }
}
