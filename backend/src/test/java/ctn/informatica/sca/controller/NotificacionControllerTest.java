package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class NotificacionControllerTest {

    @Test
    void listar_deberiaUsarUserTypePadreParaNivel4() throws Exception {
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        when(userDao.findById(44)).thenReturn(new User(44, "padre", "Padre Uno", 4));
        when(notificacionDao.listarPorUsuario(44, "padre", false)).thenReturn(List.of(Map.of("id", 1)));

        NotificacionController controller = new NotificacionController(notificacionDao, userDao);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(44L, null, List.of());

        List<Map<String, Object>> result = controller.listar(false, authentication);

        assertEquals(1, result.size());
        verify(notificacionDao).listarPorUsuario(44, "padre", false);
    }

    @Test
    void contador_deberiaUsarUserTypeProfesorParaNivelNoPadre() throws Exception {
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        when(userDao.findById(12)).thenReturn(new User(12, "profe", "Profesor Uno", 1));
        when(notificacionDao.contarNoLeidas(12, "profesor")).thenReturn(7L);

        NotificacionController controller = new NotificacionController(notificacionDao, userDao);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(12L, null, List.of());

        Map<String, Object> result = controller.contador(authentication);

        assertEquals(7L, result.get("count"));
        verify(notificacionDao).contarNoLeidas(12, "profesor");
    }

    @Test
    void marcarTodasLeidas_deberiaActualizarSoloLasDelUsuarioAutenticado() throws Exception {
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        when(userDao.findById(44)).thenReturn(new User(44, "padre", "Padre Uno", 4));
        when(notificacionDao.marcarTodasLeidas(44, "padre")).thenReturn(3);

        NotificacionController controller = new NotificacionController(notificacionDao, userDao);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(44L, null, List.of());

        Map<String, Object> result = controller.marcarTodasLeidas(authentication);

        assertEquals(true, result.get("ok"));
        assertEquals(3, result.get("actualizadas"));
        verify(notificacionDao).marcarTodasLeidas(44, "padre");
    }

    @Test
    void marcarLeida_noPermiteMarcarAMano_elRecordatorioDePlanPendiente() throws Exception {
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        when(userDao.findById(12)).thenReturn(new User(12, "profe", "Profesor Uno", 1));
        when(notificacionDao.findTipo(5, 12, "profesor")).thenReturn(java.util.Optional.of("PLAN_PENDIENTE"));
        NotificacionController controller = new NotificacionController(notificacionDao, userDao);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(12L, null, List.of());

        org.springframework.web.server.ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class, () -> controller.marcarLeida(5, authentication));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        org.mockito.Mockito.verify(notificacionDao, org.mockito.Mockito.never()).marcarLeida(5, 12, "profesor");
    }

    @Test
    void marcarLeida_siguePermitidoParaLosDemasTipos() throws Exception {
        NotificacionDao notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        when(userDao.findById(12)).thenReturn(new User(12, "profe", "Profesor Uno", 1));
        when(notificacionDao.findTipo(6, 12, "profesor")).thenReturn(java.util.Optional.of("PLAN_ACEPTADO"));
        when(notificacionDao.marcarLeida(6, 12, "profesor")).thenReturn(true);
        NotificacionController controller = new NotificacionController(notificacionDao, userDao);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(12L, null, List.of());

        Map<String, Object> result = controller.marcarLeida(6, authentication);

        assertEquals(true, result.get("ok"));
        verify(notificacionDao).marcarLeida(6, 12, "profesor");
    }
}
