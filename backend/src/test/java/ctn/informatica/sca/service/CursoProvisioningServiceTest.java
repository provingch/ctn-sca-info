package ctn.informatica.sca.service;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.AcademicPeriod;
import ctn.informatica.sca.util.PushNotificationService;

public class CursoProvisioningServiceTest {

    @Test
    public void cursoCreado_deberiaNotificarAlAdminDeEspecialidad() throws Exception {
        CursoDao cursoDao = mock(CursoDao.class);
        EspecialidadDao especialidadDao = mock(EspecialidadDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        Especialidad especialidad = new Especialidad(7, "Informática");
        Profesor admin = new Profesor();
        admin.setId(42);
        admin.setNivel(3);

        when(especialidadDao.findAll()).thenReturn(List.of(especialidad));
        when(cursoDao.listDistinctSeccionesForEspecialidad(7)).thenReturn(Set.of("A"));
        when(cursoDao.existsCurso(7, 2028, "A")).thenReturn(false);
        when(cursoDao.createCursoIfNotExists(7, 2028, "A")).thenReturn(true);
        when(profesorDao.findByEspecialidadId(7)).thenReturn(List.of(admin));

        try (MockedStatic<AcademicPeriod> period = mockStatic(AcademicPeriod.class);
             MockedStatic<PushNotificationService> push = mockStatic(PushNotificationService.class)) {
            period.when(AcademicPeriod::current).thenReturn(2026);

            CursoProvisioningService.ProvisioningResult result =
                    new CursoProvisioningService(cursoDao, especialidadDao, profesorDao)
                            .ensureCursosForPeriod();

            assertEquals(1, result.created().size());
            push.verify(() -> PushNotificationService.sendToUser(
                    42,
                    "profesor",
                    "Curso 1er año creado",
                    "Ya se creó el curso de Informática para 2028 — cargá los ingresantes cuando tengas la lista.",
                    "/admin/alumnos"));
        }
    }

    @Test
    public void sinCursosNuevos_noDeberiaNotificar() throws Exception {
        CursoDao cursoDao = mock(CursoDao.class);
        EspecialidadDao especialidadDao = mock(EspecialidadDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        Especialidad especialidad = new Especialidad(7, "Informática");

        when(especialidadDao.findAll()).thenReturn(List.of(especialidad));
        when(cursoDao.listDistinctSeccionesForEspecialidad(7)).thenReturn(Set.of("A"));
        when(cursoDao.existsCurso(7, 2028, "A")).thenReturn(true);

        try (MockedStatic<AcademicPeriod> period = mockStatic(AcademicPeriod.class);
             MockedStatic<PushNotificationService> push = mockStatic(PushNotificationService.class)) {
            period.when(AcademicPeriod::current).thenReturn(2026);

            new CursoProvisioningService(cursoDao, especialidadDao, profesorDao).ensureCursosForPeriod();

            push.verifyNoInteractions();
        }
    }
}