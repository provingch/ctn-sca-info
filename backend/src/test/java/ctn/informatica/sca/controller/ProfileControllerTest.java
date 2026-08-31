package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.PushSubscriptionDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.ChangePasswordRequest;
import ctn.informatica.sca.dto.ProfileResponse;
import ctn.informatica.sca.dto.SaveProfileRequest;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.util.PasswordUtil;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private Authentication authentication(int userId, int level) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_LEVEL_" + level)));
    }

    @Mock
    private AsignacionDao asignacionDao;

    @Mock
    private CursoDao cursoDao;

    @Mock
    private EspecialidadDao especialidadDao;

    @Mock
    private MateriaDao materiaDao;

    @Mock
    private PadreDao padreDao;

    @Mock
    private ProfesorDao profesorDao;

    @Mock
    private PushSubscriptionDao pushSubscriptionDao;

    @Mock
    private UserDao userDao;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProfileController controller;

    @Test
    void shouldChangeProfessorPasswordWhenCurrentPasswordMatches() throws Exception {
        User user = new User(1, "profesor", "Profesor Uno", 1);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Profesor profesor = new Profesor();
        profesor.setId(1);
        profesor.setContrasenia(PasswordUtil.hash("actual"));

        when(userDao.findByIdAndLevel(1, 1)).thenReturn(user);
        when(profesorDao.findById(1)).thenReturn(profesor);
        when(profesorDao.update(any())).thenReturn(true);

        controller.changePassword(new ChangePasswordRequest("actual", "nuevo123", "nuevo123"), authentication);

        ArgumentCaptor<Profesor> captor = ArgumentCaptor.forClass(Profesor.class);
        verify(profesorDao, times(1)).update(captor.capture());
        assertEquals("nuevo123", captor.getValue().getContrasenia());
    }

    @Test
    void shouldChangeParentPasswordWhenCurrentPasswordMatches() throws Exception {
        User user = new User(2, "padre", "Padre Uno", 4);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Padre padre = new Padre();
        padre.setId(2);
        padre.setContrasenia(PasswordUtil.hash("actualpadre"));

        when(userDao.findByIdAndLevel(2, 4)).thenReturn(user);
        when(padreDao.findById(2)).thenReturn(padre);
        when(padreDao.update(any())).thenReturn(true);

        controller.changePassword(new ChangePasswordRequest("actualpadre", "nuevo-padre", "nuevo-padre"), authentication);

        ArgumentCaptor<Padre> captor = ArgumentCaptor.forClass(Padre.class);
        verify(padreDao, times(1)).update(captor.capture());
        assertEquals("nuevo-padre", captor.getValue().getContrasenia());
    }

    @Test
    void shouldReturnParentProfileWhenUserIsParent() throws Exception {
        User user = new User(11, "padre", "Padre Uno", 4);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Padre padre = new Padre();
        padre.setId(11);
        padre.setNombre("Ana");
        padre.setApellido("Gomez");
        padre.setUsuario("padre");
        padre.setCorreo("padre@example.com");

        when(userDao.findByIdAndLevel(11, 4)).thenReturn(user);
        when(padreDao.findById(11)).thenReturn(padre);
        when(especialidadDao.findAll()).thenReturn(Collections.emptyList());
        when(pushSubscriptionDao.findByUser(11, "padre")).thenReturn(Collections.emptyList());

        ProfileResponse response = controller.getProfile(authentication);

        assertTrue(response.isParentProfile());
        assertFalse(response.isProfessorProfile());
        assertFalse(response.pushEnabled());
        assertEquals("Familia", response.profileRoleLabel());
        assertEquals("padre", response.profileOwner().usuario());
        verify(profesorDao, never()).findById(11);
    }

    @Test
    void shouldSaveProfessorProfileWhenValuesAreValid() throws Exception {
        User user = new User(3, "profesor", "Administrador", 3);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Profesor profesor = new Profesor();
        profesor.setId(3);
        profesor.setUsuario("profesor");

        when(userDao.findByIdAndLevel(3, 3)).thenReturn(user);
        when(profesorDao.findById(3)).thenReturn(profesor);
        when(profesorDao.update(any())).thenReturn(true);

        SaveProfileRequest request = new SaveProfileRequest(
                "profesor@example.com",
                "1234567",
                "7654321",
                "profesor",
                "Juan",
                "Perez",
                12345678,
                1,
            null,
            null
        );

        controller.saveProfile(request, authentication);

        ArgumentCaptor<Profesor> captor = ArgumentCaptor.forClass(Profesor.class);
        verify(profesorDao, times(1)).update(captor.capture());
        assertEquals("Juan", captor.getValue().getNombre());
        assertEquals("Perez", captor.getValue().getApellido());
        assertEquals("profesor@example.com", captor.getValue().getCorreo());
    }

    @Test
    void shouldLoadCoordinacionPedagogicaProfileAndRole() throws Exception {
        User user = new User(42, "coordinacion", "Coordinación Uno", 5);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Profesor profesor = new Profesor();
        profesor.setId(42);
        profesor.setUsuario("coordinacion");
        profesor.setNombre("Coordinación");
        profesor.setApellido("Uno");
        profesor.setCorreo("coord@example.com");

        when(userDao.findByIdAndLevel(42, 5)).thenReturn(user);
        when(profesorDao.findById(42)).thenReturn(profesor);
        when(especialidadDao.findAll()).thenReturn(Collections.emptyList());
        when(materiaDao.listByProfesor(42)).thenReturn(Collections.emptyList());
        when(materiaDao.listAvailableForProfesor(42)).thenReturn(Collections.emptyList());
        when(asignacionDao.findByProfesor(42)).thenReturn(Collections.emptyList());
        when(profesorDao.findManualSubjectsText(42)).thenReturn("");
        when(pushSubscriptionDao.findByUser(42, "profesor")).thenReturn(Collections.emptyList());

        ProfileResponse response = controller.getProfile(authentication);

        assertTrue(response.isStaffProfile());
        assertEquals("Coordinación Pedagógica", response.profileRoleLabel());
        assertNotNull(response.profileOwner());
        assertEquals("coordinacion", response.profileOwner().usuario());
    }

    @Test
    void shouldLogDetailedFieldChangesForCoordinacionPedagogicaProfile() throws Exception {
        User user = new User(43, "coord", "Coord", 5);
        Authentication authentication = authentication(user.getId(), user.getLevel());

        Profesor profesor = new Profesor();
        profesor.setId(43);
        profesor.setUsuario("coord");
        profesor.setCorreo("coord@viejo.com");
        profesor.setNombre("Coord");
        profesor.setApellido("Viejo");
        profesor.setCi(1234567);

        when(userDao.findByIdAndLevel(43, 5)).thenReturn(user);
        when(profesorDao.findById(43)).thenReturn(profesor);
        when(profesorDao.update(any())).thenReturn(true);

        SaveProfileRequest request = new SaveProfileRequest(
                "coord@nuevo.com",
                null,
                null,
                "coord-nuevo",
                null,
                null,
                null,
                null,
                null,
                null
        );

        controller.saveProfile(request, authentication);

        verify(activityLogService).registrar(eq(43), contains("correo: 'coord@viejo.com' → 'coord@nuevo.com'"));
        verify(activityLogService).registrar(eq(43), contains("usuario: 'coord' → 'coord-nuevo'"));
    }

    @Test
    void shouldExposeSignaturePanelForEvaluatorAndIgnoreAdminSignatureUpdate() throws Exception {
        User evaluator = new User(22, "evaluador", "Evaluador Uno", 2);
        Authentication evaluatorAuth = authentication(evaluator.getId(), evaluator.getLevel());
        Profesor evaluatorProfesor = new Profesor();
        evaluatorProfesor.setId(22);
        evaluatorProfesor.setUsuario("evaluador");

        when(userDao.findByIdAndLevel(22, 2)).thenReturn(evaluator);
        when(profesorDao.findById(22)).thenReturn(evaluatorProfesor);
        when(especialidadDao.findAll()).thenReturn(Collections.emptyList());
        when(materiaDao.listByProfesor(22)).thenReturn(Collections.emptyList());
        when(materiaDao.listAvailableForProfesor(22)).thenReturn(Collections.emptyList());
        when(asignacionDao.findByProfesor(22)).thenReturn(Collections.emptyList());
        when(profesorDao.findManualSubjectsText(22)).thenReturn("");

        ProfileResponse evaluatorResponse = controller.getProfile(evaluatorAuth);
        assertTrue(evaluatorResponse.showSignaturePanel());
        assertFalse(evaluatorResponse.isProfessorProfile());

        User admin = new User(33, "admin", "Admin Uno", 3);
        Authentication adminAuth = authentication(admin.getId(), admin.getLevel());
        Profesor adminProfesor = new Profesor();
        adminProfesor.setId(33);
        adminProfesor.setUsuario("admin");

        when(userDao.findByIdAndLevel(33, 3)).thenReturn(admin);
        when(profesorDao.findById(33)).thenReturn(adminProfesor);
        when(profesorDao.update(any())).thenReturn(true);

        controller.saveProfile(new SaveProfileRequest(
                "admin@example.com",
                null,
                null,
                "admin",
                "Admin",
                "Uno",
                123,
                3,
                "data:image/png;base64,AAA",
                null
        ), adminAuth);

        ArgumentCaptor<Profesor> captor = ArgumentCaptor.forClass(Profesor.class);
        verify(profesorDao, atLeastOnce()).update(captor.capture());
        assertNull(captor.getValue().getFirmaImagen());
    }
}
