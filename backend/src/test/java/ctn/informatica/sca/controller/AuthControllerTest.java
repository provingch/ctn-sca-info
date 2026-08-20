package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.dto.LoginResponse;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.security.JwtService;
import ctn.informatica.sca.service.AuthService;
import ctn.informatica.sca.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDao userDao;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, jwtService, refreshTokenService, userDao);
    }

    @Test
    void loginWithoutRememberMeIssuesBrowserSessionCookie() throws Exception {
        LoginRequest login = new LoginRequest("usuario", "clave", false);
        when(authService.login(login)).thenReturn(new LoginResponse(false, null, "access-token", 1));
        when(jwtService.extractUserId("access-token")).thenReturn(7L);
        when(refreshTokenService.issueToken(7, 1, null, "127.0.0.1")).thenReturn("session-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.login(login, new MockHttpServletRequest(), response);

        Collection<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("SCA_SESSION=session-token")));
        assertTrue(cookies.stream().filter(value -> value.startsWith("SCA_SESSION=session-token"))
                .noneMatch(value -> value.contains("Max-Age=")));
    }

    @Test
    void loginWithRememberMeIssuesPersistentCookie() throws Exception {
        LoginRequest login = new LoginRequest("usuario", "clave", true);
        when(authService.login(login)).thenReturn(new LoginResponse(false, null, "access-token", 1));
        when(jwtService.extractUserId("access-token")).thenReturn(7L);
        when(refreshTokenService.issueToken(7, 1, null, "127.0.0.1")).thenReturn("remember-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.login(login, new MockHttpServletRequest(), response);

        Collection<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("SCA_REMEMBER=remember-token")
                && value.contains("Max-Age=2592000")));
    }

    @Test
    void loginStillReturnsAccessTokenWhenRefreshCookiePersistenceFails() throws Exception {
        LoginRequest login = new LoginRequest("usuario", "clave", false);
        when(authService.login(login)).thenReturn(new LoginResponse(false, null, "access-token", 1));
        when(jwtService.extractUserId("access-token")).thenReturn(7L);
        when(refreshTokenService.issueToken(7, 1, null, "127.0.0.1")).thenThrow(new RuntimeException("DB unavailable"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        var result = controller.login(login, new MockHttpServletRequest(), response);

        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertTrue(result.getBody() instanceof LoginResponse);
        assertTrue(((LoginResponse) result.getBody()).accessToken().equals("access-token"));
    }

    @Test
    void refreshKeepsSessionCookieNonPersistent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RefreshTokenService.SESSION_COOKIE_NAME, "session-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.rotate("session-token", null, "127.0.0.1"))
                .thenReturn(new RefreshTokenService.RotationResult("rotated-token", 7, 1));
        when(userDao.findByIdAndLevel(7, 1)).thenReturn(new User(7, "usuario", "Usuario", 1));
        when(jwtService.generateAccessToken(7L, 1)).thenReturn("new-access-token");

        controller.refresh(request, response);

        Collection<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("SCA_SESSION=rotated-token")));
        assertFalse(cookies.stream().anyMatch(value -> value.startsWith("SCA_SESSION=rotated-token")
                && value.contains("Max-Age=")));
    }
}
