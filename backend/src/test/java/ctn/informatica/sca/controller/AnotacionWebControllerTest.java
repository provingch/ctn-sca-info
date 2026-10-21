package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AnotacionWebDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.model.Alumno;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/** Anotaciones con foto en la web: solo Coordinación (nivel 5) y el padre de cada alumno. */
class AnotacionWebControllerTest {

    private static final Authentication PADRE = new UsernamePasswordAuthenticationToken(40L, null, List.of());
    private static final int ANIO = Year.now(ZoneId.of("America/Asuncion")).getValue();

    private final AnotacionWebDao dao = mock(AnotacionWebDao.class);
    private final PadreDao padreDao = mock(PadreDao.class);
    private final AnotacionWebController controller = new AnotacionWebController(dao, padreDao);

    private static Alumno hijo(int id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        return alumno;
    }

    private static AnotacionWebDao.Anotacion anotacion(Integer promocion, Integer fotoId) {
        return new AnotacionWebDao.Anotacion(9, 5, "Ana", "Gómez", "Informática", promocion, "B",
                LocalDateTime.of(2026, 10, 21, 7, 30), "No trae corbata", null, "Programación", "Luis", "Pérez", fotoId);
    }

    @Test
    void coordinacionSoloNivel5() throws Exception {
        assertEquals("hasRole('LEVEL_5')", AnotacionWebController.class.getMethod("recientes", int.class)
                .getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('LEVEL_5')", AnotacionWebController.class.getMethod("fotoCoordinacion", int.class)
                .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void dtoArmaCursoProfesorYFoto() {
        AnotacionWebController.AnotacionDto dto = AnotacionWebController.dto(anotacion(ANIO + 1, 3));
        assertEquals("Informática · 2° B", dto.curso());
        assertEquals("Luis Pérez", dto.profesor());
        assertEquals("2026-10-21 07:30:00", dto.fechaHora());
        assertTrue(dto.tieneFoto());

        AnotacionWebController.AnotacionDto egresado = AnotacionWebController.dto(anotacion(ANIO - 1, null));
        assertEquals("Informática · Promoción " + (ANIO - 1) + " B", egresado.curso());
        assertFalse(egresado.tieneFoto());
        assertNull(AnotacionWebController.dto(anotacion(null, null)).curso());
    }

    @Test
    void recientesAcotaElLimite() throws Exception {
        when(dao.recientes(300)).thenReturn(List.of(anotacion(ANIO, null)));
        assertEquals(1, controller.recientes(5000).size());
        verify(dao).recientes(300);
    }

    @Test
    void padreVeLasAnotacionesDeSuHijo() throws Exception {
        when(padreDao.findChildrenByPadreId(40)).thenReturn(List.of(hijo(5)));
        when(dao.porAlumno(5)).thenReturn(List.of(anotacion(ANIO, 3)));
        assertEquals(1, controller.delHijo(5, PADRE).size());
    }

    @Test
    void padreNoVeAnotacionesNiFotosDeOtroAlumno() throws Exception {
        when(padreDao.findChildrenByPadreId(40)).thenReturn(List.of(hijo(5)));
        ResponseStatusException lista = assertThrows(ResponseStatusException.class, () -> controller.delHijo(6, PADRE));
        assertEquals(HttpStatus.FORBIDDEN, lista.getStatusCode());
        ResponseStatusException foto = assertThrows(ResponseStatusException.class, () -> controller.fotoPadre(6, 9, PADRE));
        assertEquals(HttpStatus.FORBIDDEN, foto.getStatusCode());
        verify(dao, never()).porAlumno(anyInt());
    }

    @Test
    void fotoDelPadreSeFiltraPorAlumno() throws Exception {
        when(padreDao.findChildrenByPadreId(40)).thenReturn(List.of(hijo(5)));
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 1};
        when(dao.foto(9, 5)).thenReturn(new AnotacionWebDao.Foto("image/jpeg", jpeg));
        assertArrayEquals(jpeg, controller.fotoPadre(5, 9, PADRE).getBody());

        // Una anotación de otro alumno no aparece aunque el padre adivine su id.
        ResponseStatusException ajena = assertThrows(ResponseStatusException.class, () -> controller.fotoPadre(5, 10, PADRE));
        assertEquals(HttpStatus.NOT_FOUND, ajena.getStatusCode());
    }
}
