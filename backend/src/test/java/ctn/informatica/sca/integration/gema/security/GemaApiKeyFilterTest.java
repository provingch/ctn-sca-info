package ctn.informatica.sca.integration.gema.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ctn.informatica.sca.security.JwtAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class GemaApiKeyFilterTest {

    private static final String VALID_KEY = "clave-super-secreta-de-gema";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private GemaProperties enabledProperties() {
        GemaProperties properties = new GemaProperties();
        properties.setClientId("gema");
        properties.setApiKey(VALID_KEY);
        return properties;
    }

    private MockHttpServletRequest requestTo(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }

    @Test
    void sinHeaderRespondeNoAutorizado() throws Exception {
        GemaApiKeyFilter filter = new GemaApiKeyFilter(enabledProperties(), new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void claveIncorrectaRespondeNoAutorizado() throws Exception {
        GemaApiKeyFilter filter = new GemaApiKeyFilter(enabledProperties(), new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        request.addHeader("X-API-Key", "clave-equivocada");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void claveCorrectaAutenticaYSigueLaCadena() throws Exception {
        GemaApiKeyFilter filter = new GemaApiKeyFilter(enabledProperties(), new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        request.addHeader("X-API-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals("gema", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("ROLE_INTEGRATION_GEMA", SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void integracionDeshabilitadaRespondeServicioNoDisponibleAunConClaveCorrecta() throws Exception {
        GemaProperties properties = new GemaProperties(); // apiKey por defecto: ""
        GemaApiKeyFilter filter = new GemaApiKeyFilter(properties, new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        request.addHeader("X-API-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(503, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void ipFueraDeLaListaPermitidaRespondeProhibido() throws Exception {
        GemaProperties properties = enabledProperties();
        properties.setAllowedIps("10.0.0.5, 10.0.0.6");
        GemaApiKeyFilter filter = new GemaApiKeyFilter(properties, new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        request.addHeader("X-API-Key", VALID_KEY);
        request.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void ipDentroDeLaListaPermitidaFunciona() throws Exception {
        GemaProperties properties = enabledProperties();
        properties.setAllowedIps("10.0.0.5, 10.0.0.6");
        GemaApiKeyFilter filter = new GemaApiKeyFilter(properties, new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/integracion/gema/ping");
        request.addHeader("X-API-Key", VALID_KEY);
        request.setRemoteAddr("10.0.0.6");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // El filtro es un @Component: Spring Boot lo registra también como filtro global (ver
    // shouldNotFilter). Sin este guard, con la integración deshabilitada (default) rechazaría
    // cualquier request de toda la aplicación, no solo las de GEMA.
    @Test
    void noActuaFueraDeLasRutasDeIntegracion() throws Exception {
        GemaProperties properties = new GemaProperties(); // deshabilitada
        GemaApiKeyFilter filter = new GemaApiKeyFilter(properties, new JwtAuthenticationEntryPoint());
        MockHttpServletRequest request = requestTo("/api/home");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // A través de doFilter (no doFilterInternal): así el shouldNotFilter de OncePerRequestFilter
        // efectivamente se consulta, igual que en un request real.
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus()); // sin tocar: shouldNotFilter cortó antes de authorize()
        verify(chain).doFilter(request, response);
    }
}
