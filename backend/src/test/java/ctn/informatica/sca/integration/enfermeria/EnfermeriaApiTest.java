package ctn.informatica.sca.integration.enfermeria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import ctn.informatica.sca.security.JwtService;
import ctn.informatica.sca.util.PasswordUtil;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** API de enfermería contra el contexto real de Spring (seguridad incluida), con el DAO mockeado. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"enfermeria.api-key=clave-enf-test", "anotaciones.api-key=clave-anot-test"})
class EnfermeriaApiTest {

    private static final String KEY = "clave-enf-test";
    private static final String HASH = PasswordUtil.hash("secreta123");
    private static final EnfermeriaDao.Usuario ENFERMERA = new EnfermeriaDao.Usuario(5, "ana", HASH, "enfermeria");
    private static final EnfermeriaDao.Usuario ADMIN = new EnfermeriaDao.Usuario(1, "admin", HASH, "administrador");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnfermeriaSecurityConfig.Tokens tokens;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DatabaseMigrationInitializer databaseMigrationInitializer;

    @MockitoBean
    private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;

    @MockitoBean
    private EnfermeriaDao dao;

    @Test
    void sinClaveOConLaDeAnotacionesDa401() throws Exception {
        mockMvc.perform(post("/api/enfermeria/login").param("usuario", "ana").param("contrasena", "secreta123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Clave de la app inválida o ausente"));
        mockMvc.perform(post("/api/enfermeria/login").header("X-API-Key", "clave-anot-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDevuelveTokenQueAbreLaFicha() throws Exception {
        when(dao.usuarioPorNombre("ana")).thenReturn(ENFERMERA);
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.ficha(anyInt(), anyInt())).thenReturn(Map.of("id", 9, "enfermedadBase", "Asma"));

        String body = mockMvc.perform(post("/api/enfermeria/login").header("X-API-Key", KEY)
                        .param("usuario", "ana").param("contrasena", "secreta123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.rol").value("enfermeria"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.datos.token");

        mockMvc.perform(get("/api/enfermeria/ficha").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + token).param("alumno", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.enfermedadBase").value("Asma"));
    }

    @Test
    void contrasenaMalDa401() throws Exception {
        when(dao.usuarioPorNombre("ana")).thenReturn(ENFERMERA);
        mockMvc.perform(post("/api/enfermeria/login").header("X-API-Key", KEY)
                        .param("usuario", "ana").param("contrasena", "otra-cosa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void soloConLaClaveNoSeLeenFichas() throws Exception {
        mockMvc.perform(get("/api/enfermeria/ficha").header("X-API-Key", KEY).param("alumno", "9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Falta iniciar sesión"));
        verify(dao, never()).ficha(anyInt(), anyInt());
    }

    @Test
    void tokensDelColegioYDeEnfermeriaNoSeMezclan() throws Exception {
        String colegio = jwtService.generateAccessToken(5L, 3, 0);
        mockMvc.perform(get("/api/enfermeria/alergias").header("X-API-Key", KEY).header("Authorization", "Bearer " + colegio))
                .andExpect(status().isUnauthorized());
        // Y el de enfermería no abre el panel de admin del colegio aunque el id coincida.
        mockMvc.perform(get("/api/admin/alumnos").header("Authorization", "Bearer " + tokens.emitir(ENFERMERA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarContrasenaInvalidaElTokenViejo() throws Exception {
        String viejo = tokens.emitir(ENFERMERA);
        when(dao.usuarioPorId(5)).thenReturn(new EnfermeriaDao.Usuario(5, "ana", PasswordUtil.hash("nueva-clave"), "enfermeria"));
        mockMvc.perform(get("/api/enfermeria/alergias").header("X-API-Key", KEY).header("Authorization", "Bearer " + viejo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElAdministradorCreaUsuarios() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.usuarioPorId(1)).thenReturn(ADMIN);
        when(dao.crearUsuario(anyString(), anyString(), anyString())).thenReturn(7);

        mockMvc.perform(post("/api/enfermeria/usuario_crear").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + tokens.emitir(ENFERMERA))
                        .param("usuario", "beto").param("contrasena", "12345678").param("rol", "enfermeria"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
        mockMvc.perform(post("/api/enfermeria/usuario_crear").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + tokens.emitir(ADMIN))
                        .param("usuario", "beto").param("contrasena", "12345678").param("rol", "enfermeria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.id_usuario").value(7));
    }

    @Test
    void fichaGuardarValidaAlumnoYAlergias() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        String token = "Bearer " + tokens.emitir(ENFERMERA);

        when(dao.existeAlumno(9)).thenReturn(true);
        when(dao.contarAlergias(List.of(1, 99))).thenReturn(1);
        mockMvc.perform(post("/api/enfermeria/ficha_guardar").header("X-API-Key", KEY).header("Authorization", token)
                        .param("alumno", "9").param("alergias", "1, 99"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post("/api/enfermeria/ficha_guardar").header("X-API-Key", KEY).header("Authorization", token)
                        .param("alumno", "9").param("alergias", "1,x"))
                .andExpect(status().isUnprocessableContent());
        verify(dao, never()).guardarFicha(anyInt(), any(), any(), any(), any(), any(), any());

        when(dao.contarAlergias(List.of(1))).thenReturn(1);
        mockMvc.perform(post("/api/enfermeria/ficha_guardar").header("X-API-Key", KEY).header("Authorization", token)
                        .param("alumno", "9").param("alergias", "1,1").param("enfermedadBase", " Asma ")
                        .param("tutor_nombre", "Rosa"))
                .andExpect(status().isOk());
        verify(dao).guardarFicha(9, "Asma", List.of(1), "Rosa", null, null, null);
    }

    @Test
    void consultaCrearSumaInsumosRepetidosYUsaElUsuarioDeLaSesion() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.existeAlumno(9)).thenReturn(true);
        when(dao.crearConsulta(any(), any(), anyInt())).thenReturn(40);

        mockMvc.perform(post("/api/enfermeria/consulta_crear").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + tokens.emitir(ENFERMERA))
                        .param("alumno", "9").param("motivo", "Fiebre").param("fecha", "2026-10-20")
                        .param("horaEntrada", "08:30").param("insumos", "3:2, 5:1,3:1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idConsulta").value(40));
        verify(dao).crearConsulta(new EnfermeriaDao.DatosConsulta(9, "Fiebre", java.time.LocalDate.of(2026, 10, 20), null,
                java.time.LocalTime.of(8, 30), null, null), Map.of(3, 3, 5, 1), 5);
    }

    @Test
    void consultaConDatosMalFormadosDa422YNoTocaLaBase() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.existeAlumno(9)).thenReturn(true);
        String token = "Bearer " + tokens.emitir(ENFERMERA);
        String[][] malos = {{"fecha", "20/10/2026"}, {"insumos", "3"}, {"insumos", "3:0"}, {"horaEntrada", "8h"}, {"motivo", " "}};
        for (String[] malo : malos) {
            Map<String, String> params = new java.util.HashMap<>(Map.of("alumno", "9", "motivo", "Fiebre", "fecha", "2026-10-20"));
            params.put(malo[0], malo[1]);
            var req = post("/api/enfermeria/consulta_crear").header("X-API-Key", KEY).header("Authorization", token);
            params.forEach(req::param);
            mockMvc.perform(req)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.ok").value(false));
        }
        verify(dao, never()).crearConsulta(any(), any(), anyInt());
    }

    @Test
    void stockInsuficienteSaleComo409ConMensaje() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.existeAlumno(9)).thenReturn(true);
        when(dao.crearConsulta(any(), any(), anyInt())).thenThrow(
                new EnfermeriaDao.Rechazo(org.springframework.http.HttpStatus.CONFLICT, "No hay stock suficiente de Gasa (quedan 1)"));
        mockMvc.perform(post("/api/enfermeria/consulta_crear").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + tokens.emitir(ENFERMERA))
                        .param("alumno", "9").param("motivo", "Corte").param("fecha", "2026-10-20").param("insumos", "3:5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("No hay stock suficiente de Gasa (quedan 1)"));
    }

    @Test
    void solicitudAdmiteDosPuntosEnLaObservacion() throws Exception {
        when(dao.usuarioPorId(5)).thenReturn(ENFERMERA);
        when(dao.crearSolicitud(any(), any(), anyInt())).thenReturn(3);
        mockMvc.perform(post("/api/enfermeria/solicitud_crear").header("X-API-Key", KEY)
                        .header("Authorization", "Bearer " + tokens.emitir(ENFERMERA))
                        .param("items", "4:10:urgente: para el lunes\n7:2\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.id_solicitud").value(3));
        verify(dao).crearSolicitud(eq(List.of(new EnfermeriaDao.ItemSolicitud(4, 10, "urgente: para el lunes"),
                new EnfermeriaDao.ItemSolicitud(7, 2, null))), any(), eq(5));
    }
}
