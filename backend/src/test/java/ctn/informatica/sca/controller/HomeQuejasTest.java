package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.*;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeQuejasTest {
    private final CursoBaseDao cursos = mock(CursoBaseDao.class);
    private final AsignacionDao asignaciones = mock(AsignacionDao.class);
    private final ProfesorDao profesores = mock(ProfesorDao.class);
    private final UserDao usuarios = mock(UserDao.class);
    private final QuejaDao quejas = mock(QuejaDao.class);
    private final Authentication auth = new UsernamePasswordAuthenticationToken(9, null);
    private final HomeController controller = new HomeController(
            null, cursos, asignaciones, profesores, null, null, null, null, null,
            usuarios, null, null, null, mock(ConfiguracionSistemaDao.class), null,
            mock(NotificacionDao.class), quejas);

    @BeforeEach
    void setup() throws Exception {
        when(usuarios.findById(9)).thenReturn(new User(9, "admin", "Admin", 3));
        Profesor admin = new Profesor();
        admin.setNivel(3);
        when(profesores.findById(9)).thenReturn(admin);
        when(cursos.findEspecialidadId(13)).thenReturn(21);
        when(asignaciones.findByProfesorAndCurso(7, 13)).thenReturn(List.of(new Asignacion(1, 7, 2, 13)));
        when(quejas.crear(7, 13, 21, "Motivo", 9)).thenReturn(42);
    }

    @Test
    void derivaEspecialidadDelCursoAunqueElClienteEnvieOtra() throws Exception {
        controller.registrarQueja(Map.of("cursoId", 13, "profesorId", 7, "especialidadId", 999, "motivo", "  Motivo  "), auth);
        verify(quejas).crear(7, 13, 21, "Motivo", 9);
    }

    @Test
    void permiteRegistrarSinEspecialidadEnElPayload() throws Exception {
        controller.registrarQueja(payload("Motivo"), auth);
        verify(quejas).crear(7, 13, 21, "Motivo", 9);
    }

    @Test
    void rechazaCursoFueraDelAlcanceAdministrativo() throws Exception {
        Profesor admin = new Profesor();
        admin.setNivel(3);
        admin.setEspecialidadId(99);
        when(profesores.findById(9)).thenReturn(admin);
        assertStatus(403, payload("Motivo"));
    }

    @Test
    void rechazaProfesorSinAsignacion() throws Exception {
        when(asignaciones.findByProfesorAndCurso(7, 13)).thenReturn(List.of());
        assertStatus(400, payload("Motivo"));
    }

    @Test
    void rechazaCursoInexistente() throws Exception {
        when(cursos.findEspecialidadId(13)).thenReturn(null);
        assertStatus(400, payload("Motivo"));
    }

    @Test
    void rechazaMotivoEnBlanco() {
        assertStatus(400, payload("   "));
    }

    @Test
    void falloDeNotificacionNoInformaFalloDeRegistro() throws Exception {
        when(quejas.contarPorProfesor(7)).thenThrow(new SQLException("Notificación no disponible"));
        assertDoesNotThrow(() -> controller.registrarQueja(payload("Motivo"), auth));
        verify(quejas, times(1)).crear(7, 13, 21, "Motivo", 9);
    }

    private Map<String, Object> payload(String motivo) {
        return Map.of("cursoId", 13, "profesorId", 7, "motivo", motivo);
    }

    private void assertStatus(int expected, Map<String, Object> payload) {
        var error = assertThrows(ResponseStatusException.class, () -> controller.registrarQueja(payload, auth));
        assertEquals(expected, error.getStatusCode().value());
        verifyNoInteractions(quejas);
    }
}
