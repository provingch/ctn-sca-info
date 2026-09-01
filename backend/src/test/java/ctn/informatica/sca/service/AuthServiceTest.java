package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserDao userDao;
    @Mock private PadreDao padreDao;
    @Mock private ProfesorDao profesorDao;
    @Mock private JwtService jwtService;
    @Mock private LoginAttemptService loginAttemptService;

    @Test
    void loginFailsClosedWhenTotpConfigurationCannotBeRead() throws Exception {
        User user = new User(7, "profesor", "Profesor", 1);
        when(userDao.findByUsernameAndPassword("profesor", "correcta")).thenReturn(user);
        when(profesorDao.findById(7)).thenThrow(new RuntimeException("DB unavailable"));
        AuthService service = new AuthService(userDao, padreDao, profesorDao, jwtService, loginAttemptService);

        AuthService.AuthException error = assertThrows(AuthService.AuthException.class,
                () -> service.login(new LoginRequest("profesor", "correcta", false), "127.0.0.1"));

        assertEquals(503, error.status());
    }
}
