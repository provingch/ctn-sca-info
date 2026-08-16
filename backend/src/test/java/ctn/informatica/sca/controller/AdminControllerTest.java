package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.GradeDao;
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

        when(gradeDao.deleteGradesForPlanilla(42)).thenReturn(5);
        when(tareaDao.deleteImportedTasks(42)).thenReturn(3);

        AdminController controller = new AdminController(tareaDao, gradeDao);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1);

        AdminController.WipeResponse resp = controller.wipePlanillaSync(42, auth);

        assertNotNull(resp);
        assertEquals(42, resp.planillaId());
        assertEquals(5, resp.deletedGrades());
        assertEquals(3, resp.deletedTasks());
        assertTrue(resp.message().contains("Wipe"));

        verify(gradeDao).deleteGradesForPlanilla(42);
        verify(tareaDao).deleteImportedTasks(42);
    }
}
