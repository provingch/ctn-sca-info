package ctn.informatica.sca.integration.gema.controller;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regresión de la cadena de seguridad existente al agregar la de GEMA, contra el contexto real de
 * Spring (mismas mocks de arranque que {@code ScaApplicationTests} para no depender de una base
 * real). {@code gema.integration.api-key} se fija por propiedad para no depender de la variable
 * de entorno del entorno donde corra el test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gema.integration.api-key=clave-de-test-de-integracion")
class GemaPingIntegrationTest {

    private static final String API_KEY = "clave-de-test-de-integracion";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Qualifier("gemaIntegrationFilterChain")
    private SecurityFilterChain gemaFilterChain;

    @MockitoBean
    private DatabaseMigrationInitializer databaseMigrationInitializer;

    @MockitoBean
    private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;

    // JwtAuthenticationFilter consulta la sesión contra la base real; se mockea para que la
    // autenticación JWT no dependa de una base disponible en este test.
    @MockitoBean
    private UserDao userDao;

    @Test
    void saludPublicaSigueSinAutenticar() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void pingSinClaveRespondeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/integracion/gema/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingConClaveIncorrectaRespondeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/integracion/gema/ping").header("X-API-Key", "clave-equivocada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingConClaveCorrectaAutentica() throws Exception {
        // Sin base real en este entorno de test: puede dar 200 (base disponible) o 503
        // (database:"error"), pero nunca 401/403 — eso es lo que prueba que la clave autenticó.
        mockMvc.perform(get("/api/integracion/gema/ping").header("X-API-Key", API_KEY))
                .andExpect(status().is(anyOf(is(200), is(503))))
                .andExpect(content().string(containsString("\"client\":\"gema\"")));
    }

    @Test
    void pingConClaveCorrectaNuncaDaNoAutorizadoNiProhibido() throws Exception {
        mockMvc.perform(get("/api/integracion/gema/ping").header("X-API-Key", API_KEY))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    /**
     * Regresión de la cadena existente: en vez de un round-trip completo por {@code /api/home}
     * (que expone una fragilidad preexistente e independiente de esta integración — ver nota más
     * abajo), se prueba directamente el {@code securityMatcher} de la cadena nueva: por
     * construcción de {@code FilterChainProxy}, una ruta que esta cadena NO reclama cae en la
     * única otra cadena registrada (la de {@code SecurityConfig}, sin tocar), así que esto basta
     * para probar que rutas existentes no pasan por la cadena de GEMA.
     *
     * <p><b>Nota (hallazgo de testing, no un bug de producción):</b> {@code JwtAuthenticationFilter}
     * es un {@code @Component} (registrado también como filtro global, ver {@code GemaApiKeyFilter})
     * Y se agrega vía {@code addFilterBefore} dentro de la cadena de {@code SecurityConfig}.
     * {@code OncePerRequestFilter} marca "ya filtrado" por NOMBRE DE CLASE, no por instancia, así
     * que si la invocación global corriera antes que la de la cadena interna, esta última se
     * saltaría. Bajo {@code @AutoConfigureMockMvc} eso pasa de verdad — reproducido con un test
     * aislado sin ningún código de esta integración, {@code GET /api/home} con Bearer válido da 401
     * incluso sin este paquete — pero es un artefacto de cómo
     * {@code SpringBootMockMvcBuilderCustomizer} arma la lista de filtros para MockMvc, no del
     * orden real de un Tomcat embebido: verificado a mano contra un Tomcat real (login real +
     * {@code GET /api/home} con el token emitido) y responde 200 normalmente. Por eso esta
     * regresión se prueba acá por matcher en vez de por MockMvc, y por eso no se toca
     * {@code SecurityConfig}/{@code JwtAuthenticationFilter}: no hay nada que corregir en la app.
     */
    @Test
    void laCadenaDeGemaNoReclamaRutasExistentes() {
        assertTrue(gemaFilterChain.matches(requestTo("/api/integracion/gema/ping")));
        assertFalse(gemaFilterChain.matches(requestTo("/api/home")));
        assertFalse(gemaFilterChain.matches(requestTo("/api/auth/login")));
        assertFalse(gemaFilterChain.matches(requestTo("/api/health")));
    }

    private static MockHttpServletRequest requestTo(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void pingConAuthorizationBearerPeroSinApiKeySigueRespondiendoNoAutorizado() throws Exception {
        // JwtAuthenticationFilter (registrado también como filtro global) ignora este header
        // porque no hay X-API-Key: no debe colarse como autenticado en la cadena de GEMA.
        when(userDao.findSessionState(7)).thenReturn(new UserDao.SessionState(1, 3));
        String token = jwtService.generateAccessToken(7L, 1, 3);

        mockMvc.perform(get("/api/integracion/gema/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
