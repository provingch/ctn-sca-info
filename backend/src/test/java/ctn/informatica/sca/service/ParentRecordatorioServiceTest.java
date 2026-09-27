package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParentRecordatorioServiceTest {

    private NotificacionDao notificacionDao;
    private ParentPushService parentPushService;
    private FirebaseMessagingClient fcm;
    private AlumnoDao alumnoDao;
    private ParentRecordatorioService service;

    private static final LocalDate DIA_HABIL = LocalDate.of(2026, 9, 20);

    @BeforeEach
    void setUp() throws Exception {
        notificacionDao = mock(NotificacionDao.class);
        parentPushService = mock(ParentPushService.class);
        fcm = mock(FirebaseMessagingClient.class);
        alumnoDao = mock(AlumnoDao.class);
        service = new ParentRecordatorioService(notificacionDao, parentPushService, fcm, alumnoDao);
        when(fcm.isEnabled()).thenReturn(true);
    }

    @Test
    void sinPendientesNoLlamaAFcm() throws Exception {
        when(notificacionDao.listarAlumnosPendientesPorPadre()).thenReturn(Map.of());

        int enviados = service.enviarRecordatorios(DIA_HABIL);

        assertEquals(0, enviados);
        verify(parentPushService, never()).deliver(anyList(), anyString(), anyString(), anyMap());
    }

    @Test
    void dosHijosDelMismoPadreGeneranUnSoloPush() throws Exception {
        when(notificacionDao.listarAlumnosPendientesPorPadre()).thenReturn(Map.of(10, List.of(5, 6)));

        int enviados = service.enviarRecordatorios(DIA_HABIL);

        assertEquals(1, enviados);
        verify(parentPushService, times(1)).deliver(eq(List.of(10)), anyString(),
                eq("Hay novedades de tus hijos para revisar"), anyMap());
    }

    @Test
    void unSoloHijoMencionaSuNombreEnElMensaje() throws Exception {
        when(notificacionDao.listarAlumnosPendientesPorPadre()).thenReturn(Map.of(10, List.of(5)));
        Alumno alumno = new Alumno();
        alumno.setNombre("Ana");
        when(alumnoDao.findById(5)).thenReturn(alumno);

        service.enviarRecordatorios(DIA_HABIL);

        verify(parentPushService).deliver(eq(List.of(10)), anyString(),
                eq("Hay novedades de Ana para revisar"), anyMap());
    }

    @Test
    void sinFcmHabilitadoNoConsultaPendientes() throws Exception {
        when(fcm.isEnabled()).thenReturn(false);

        assertEquals(0, service.enviarRecordatorios(DIA_HABIL));
        verify(notificacionDao, never()).listarAlumnosPendientesPorPadre();
    }

    @Test
    void fueraDelCicloLectivoNoAvisa() throws Exception {
        assertEquals(0, service.enviarRecordatorios(LocalDate.of(2026, 1, 15)));
        assertEquals(0, service.enviarRecordatorios(LocalDate.of(2026, 12, 5)));
        verify(notificacionDao, never()).listarAlumnosPendientesPorPadre();
    }

    @Test
    void unPadreConErrorNoFrenaElRecordatorioDeLosDemas() throws Exception {
        when(notificacionDao.listarAlumnosPendientesPorPadre()).thenReturn(Map.of(10, List.of(5), 11, List.of(6)));
        org.mockito.Mockito.doThrow(new RuntimeException("token vencido"))
                .when(parentPushService).deliver(eq(List.of(10)), anyString(), anyString(), anyMap());

        int enviados = service.enviarRecordatorios(DIA_HABIL);

        assertEquals(1, enviados);
        verify(parentPushService).deliver(eq(List.of(11)), anyString(), any(), anyMap());
    }
}
