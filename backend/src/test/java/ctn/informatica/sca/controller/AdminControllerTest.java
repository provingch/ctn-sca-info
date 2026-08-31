package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.QuejaDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

public class AdminControllerTest {

    private static final class ScopedAdminController extends AdminController {
        private final Integer specialtyId;

        private ScopedAdminController(TareaDao tareaDao, GradeDao gradeDao, PlanillaDao planillaDao, QuejaDao quejaDao, Integer specialtyId) {
            super(tareaDao, gradeDao, planillaDao, quejaDao);
            this.specialtyId = specialtyId;
        }

        @Override
        protected Integer getSpecialtyAdminIdForUser(int userId) {
            return specialtyId;
        }
    }

    @Test
    public void wipePlanillaSync_invokesDaos_andReturnsCounts() throws Exception {
        GradeDao gradeDao = mock(GradeDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        PlanillaDao planillaDao = mock(PlanillaDao.class);

        when(gradeDao.deleteGradesForPlanilla(42)).thenReturn(5);
        when(tareaDao.deleteImportedTasks(42)).thenReturn(3);
        when(planillaDao.updateClassroomCourseId(42, null)).thenReturn(true);

        AdminController controller = new AdminController(tareaDao, gradeDao, planillaDao);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1);

        AdminController.WipeResponse resp = controller.wipePlanillaSync(42, auth);

        assertNotNull(resp);
        assertEquals(42, resp.planillaId());
        assertEquals(5, resp.deletedGrades());
        assertEquals(3, resp.deletedTasks());
        assertEquals(1, resp.clearedGoogleCourseIds());
        assertTrue(resp.message().contains("Wipe"));

        verify(gradeDao).deleteGradesForPlanilla(42);
        verify(tareaDao).deleteImportedTasks(42);
        verify(planillaDao).updateClassroomCourseId(42, null);
    }

    @Test
    public void wipeAllClassroomSync_invokesDaos_andReturnsCounts() throws Exception {
        GradeDao gradeDao = mock(GradeDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        PlanillaDao planillaDao = mock(PlanillaDao.class);

        when(gradeDao.deleteImportedGradesForAllPlans()).thenReturn(12);
        when(tareaDao.deleteImportedTasks(null)).thenReturn(9);
        when(planillaDao.clearClassroomCourseIds()).thenReturn(4);

        AdminController controller = new AdminController(tareaDao, gradeDao, planillaDao);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1);

        AdminController.GlobalWipeResponse resp = controller.wipeAllClassroomSync(auth);

        assertNotNull(resp);
        assertEquals(12, resp.deletedGrades());
        assertEquals(9, resp.deletedTasks());
        assertEquals(4, resp.clearedGoogleCourseIds());
        assertTrue(resp.message().contains("Wipe global"));

        verify(gradeDao).deleteImportedGradesForAllPlans();
        verify(tareaDao).deleteImportedTasks(null);
        verify(planillaDao).clearClassroomCourseIds();
    }

    @Test
    public void globalAdminCanCreateSpecialtyAdmin() {
        assertDoesNotThrow(() -> AdminController.validateAdminRoleAssignment(null, 3, 7));
        assertDoesNotThrow(() -> AdminController.validateAdminMutationAccess(null, 7, 3));
    }

    @Test
    public void globalAdminCanEditSpecialtyAdminButNotGlobalAdmin() {
        assertDoesNotThrow(() -> AdminController.validateAdminMutationAccess(null, 7, 3));

        ResponseStatusException globalAdminEx = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminMutationAccess(null, null, 3));
        assertEquals(403, globalAdminEx.getStatusCode().value());
    }

    @Test
    public void specialtyAdminCannotCreateOrEditOtherAdmin() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminRoleAssignment(7, 3, 5));
        assertEquals(403, ex.getStatusCode().value());

        ResponseStatusException coordinationEx = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminRoleAssignment(7, 5, null));
        assertEquals(403, coordinationEx.getStatusCode().value());

        ResponseStatusException editEx = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminMutationAccess(7, 5, 3));
        assertEquals(403, editEx.getStatusCode().value());

        ResponseStatusException coordinationEditEx = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminMutationAccess(7, null, 5));
        assertEquals(403, coordinationEditEx.getStatusCode().value());

        ResponseStatusException selfEditEx = assertThrows(ResponseStatusException.class,
                () -> AdminController.validateAdminMutationAccess(7, 7, 3));
        assertEquals(403, selfEditEx.getStatusCode().value());
    }

    @Test
    public void listarQuejas_scopedAdminOnlySeesItsOwnSpecialty() throws Exception {
        QuejaDao quejaDao = mock(QuejaDao.class);
        when(quejaDao.listar()).thenReturn(List.of(
                Map.of("id", 1L, "especialidadId", 7),
                Map.of("id", 2L, "especialidadId", 8),
                Map.of("id", 3L, "especialidadId", 7)));

        AdminController controller = new ScopedAdminController(mock(TareaDao.class), mock(GradeDao.class), mock(PlanillaDao.class), quejaDao, 7);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1);

        List<Map<String, Object>> result = controller.listarQuejas(auth);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(row -> ((Number) row.get("especialidadId")).intValue() == 7));
        verify(quejaDao).listar();
    }
}
