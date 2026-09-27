package ctn.informatica.sca.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParentPushServiceTest {

    private PadreDao padreDao;
    private NotificacionDao notificacionDao;
    private ParentPushService service;

    @BeforeEach
    void setUp() throws Exception {
        padreDao = mock(PadreDao.class);
        notificacionDao = mock(NotificacionDao.class);
        service = new ParentPushService(mock(FirebaseMessagingClient.class), padreDao, notificacionDao);
    }

    private static Padre padre(int id) {
        Padre p = new Padre();
        p.setId(id);
        return p;
    }

    @Test
    void dosNovedadesSeguidasParaElMismoAlumnoDejanUnaSolaFilaSinLeer() throws Exception {
        when(padreDao.findPadresByAlumnoId(5)).thenReturn(List.of(padre(1)));
        // Primera vez no hay nada pendiente; la segunda vez ya existe la fila sin leer.
        when(notificacionDao.existePendienteParaUsuario(1, "padre", NotificacionDao.TIPO_NOVEDAD_ALUMNO, NotificacionDao.ENTIDAD_ALUMNO, 5))
                .thenReturn(false, true);

        service.registrarNovedadAlumnos(Set.of(5));
        service.registrarNovedadAlumnos(Set.of(5));

        verify(notificacionDao).crear(eq(1), eq("padre"), eq(NotificacionDao.TIPO_NOVEDAD_ALUMNO),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                eq(NotificacionDao.ENTIDAD_ALUMNO), eq(5L));
    }

    @Test
    void unPadreConProblemasNoFrenaElRegistroDeOtroAlumno() throws Exception {
        when(padreDao.findPadresByAlumnoId(5)).thenThrow(new java.sql.SQLException("boom"));
        when(padreDao.findPadresByAlumnoId(6)).thenReturn(List.of(padre(2)));
        when(notificacionDao.existePendienteParaUsuario(2, "padre", NotificacionDao.TIPO_NOVEDAD_ALUMNO, NotificacionDao.ENTIDAD_ALUMNO, 6))
                .thenReturn(false);

        service.registrarNovedadAlumnos(Set.of(5, 6));

        verify(notificacionDao).crear(eq(2), eq("padre"), eq(NotificacionDao.TIPO_NOVEDAD_ALUMNO),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                eq(NotificacionDao.ENTIDAD_ALUMNO), eq(6L));
        verify(notificacionDao, never()).crear(eq(1), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}
