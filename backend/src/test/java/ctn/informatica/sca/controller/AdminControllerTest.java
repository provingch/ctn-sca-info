package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminControllerTest {

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
}
