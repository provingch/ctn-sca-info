package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalaControllerSafetyTest {
    @Test void returnsConflictWhenRoomHasScheduleBlocks() throws Exception {
        AdminController controller = new AdminController(mock(TareaDao.class), mock(GradeDao.class), mock(PlanillaDao.class)) {
            @Override protected Integer getSpecialtyAdminIdForUser(int id) { return null; }
        };
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1);
        try (var ignored = mockConstruction(SalaDao.class, (dao, context) -> when(dao.eliminar(4)).thenThrow(new SalaDao.SalaEnUsoException()))) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.deleteSala(4, auth));
            assertEquals(409, error.getStatusCode().value());
            assertTrue(error.getReason().contains("bloques"));
        }
    }
}
