package ctn.informatica.sca.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.ConfiguracionSistemaDao;
import ctn.informatica.sca.dao.CursoBaseDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.QuejaDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.TemaVerificacionService;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeControllerTest {

    @Test
    void tercerAtrasoCreaUnSoloIncumplimientoPendienteYCuartoNoDuplica() throws Exception {
        RasgoPlanillaDao rasgoPlanillaDao = mock(RasgoPlanillaDao.class);
        ConfiguracionSistemaDao configuracionSistemaDao = mock(ConfiguracionSistemaDao.class);
        IncumplimientoRevisionDao incumplimientoRevisionDao = mock(IncumplimientoRevisionDao.class);
        UserDao userDao = mock(UserDao.class);

        when(configuracionSistemaDao.getInt("umbral_atrasos_incumplimiento", 3)).thenReturn(3);
        when(rasgoPlanillaDao.contarAtrasosPorAsignacionYUsuario(41, 17)).thenReturn(3, 4);
        when(incumplimientoRevisionDao.existePendientePorAsignacionYUsuario(41, 17, "ATRASO"))
                .thenReturn(false, true);
        when(userDao.findAllByLevel(2)).thenReturn(List.of());

        HomeController controller = new HomeController(
                mock(CursoDao.class), mock(CursoBaseDao.class), mock(ProfesorDao.class), mock(PlanillaDao.class),
                mock(MateriaDao.class), mock(AlumnoDao.class), rasgoPlanillaDao, mock(InstrumentoDao.class),
                userDao, mock(PlanCurricularDao.class), mock(TemaVerificacionService.class), mock(ActivityLogService.class),
                configuracionSistemaDao, incumplimientoRevisionDao, mock(NotificacionDao.class), mock(QuejaDao.class));

        controller.registrarIncumplimientoPorAtraso(41, 17, 9, "Atraso justificado 3");
        controller.registrarIncumplimientoPorAtraso(41, 17, 10, "Atraso justificado 4");

        verify(rasgoPlanillaDao, times(2)).contarAtrasosPorAsignacionYUsuario(41, 17);
        verify(incumplimientoRevisionDao, times(2)).existePendientePorAsignacionYUsuario(41, 17, "ATRASO");
        verify(incumplimientoRevisionDao, times(1)).registrar(
                eq(41), eq(17), eq(9), eq("ATRASO"), any(String.class), eq("PENDIENTE"),
                eq(null), eq(null), eq(null));
        verify(userDao, times(1)).findAllByLevel(2);
    }
}
