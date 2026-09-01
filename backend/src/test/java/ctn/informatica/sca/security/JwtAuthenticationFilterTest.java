package ctn.informatica.sca.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.UserDao;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesWhenSessionVersionMatches() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 60, 5);
        UserDao userDao = mock(UserDao.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDao);
        MockHttpServletRequest request = requestWith(jwtService.generateAccessToken(7L, 1, 3));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(userDao.findSessionState(7)).thenReturn(new UserDao.SessionState(1, 3));

        filter.doFilterInternal(request, response, chain);

        assertEquals(7L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsAccessTokenIssuedBeforePasswordChange() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 60, 5);
        UserDao userDao = mock(UserDao.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDao);
        MockHttpServletRequest request = requestWith(jwtService.generateAccessToken(7L, 1, 2));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(userDao.findSessionState(7)).thenReturn(new UserDao.SessionState(1, 3));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWith(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
