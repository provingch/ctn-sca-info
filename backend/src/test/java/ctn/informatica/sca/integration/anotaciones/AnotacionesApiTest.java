package ctn.informatica.sca.integration.anotaciones;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** API de la app de anotaciones contra el contexto real de Spring, con el DAO mockeado. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "anotaciones.api-key=clave-app-test")
class AnotacionesApiTest {

    private static final String KEY = "clave-app-test";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseMigrationInitializer databaseMigrationInitializer;

    @MockitoBean
    private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;

    @MockitoBean
    private AnotacionesDao dao;

    @Test
    void sinClaveDevuelve401EnFormatoDeLaApp() throws Exception {
        mockMvc.perform(get("/api/anotaciones/login").param("ci", "123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Clave de la app inválida o ausente"));
    }

    @Test
    void loginDevuelveProfesorEnvueltoEnOkDatos() throws Exception {
        Map<String, Object> profe = new LinkedHashMap<>();
        profe.put("id", 7);
        profe.put("nombre", "Ana");
        when(dao.profesorPorCi(123L)).thenReturn(profe);

        mockMvc.perform(get("/api/anotaciones/login").header("X-API-Key", KEY).param("ci", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.datos.id").value(7));
    }

    @Test
    void loginInexistenteYParametrosInvalidos() throws Exception {
        when(dao.profesorPorCi(999L)).thenReturn(null);
        mockMvc.perform(get("/api/anotaciones/login").header("X-API-Key", KEY).param("ci", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe un profesor con esa C.I."));
        mockMvc.perform(get("/api/anotaciones/login").header("X-API-Key", KEY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.ok").value(false));
        mockMvc.perform(get("/api/anotaciones/login").header("X-API-Key", KEY).param("ci", "abc"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void guardaConFotoEnHoraDeParaguay() throws Exception {
        existenTodos(new AnotacionesDao.Tipo(false, false, false));
        when(dao.insertar(eq(1), eq(2), eq(3), isNull(), any(), isNull(), any())).thenReturn(10);

        LocalDateTime antes = LocalDateTime.now(AnotacionesController.PARAGUAY).withNano(0);
        mockMvc.perform(guardar().file(new MockMultipartFile("foto", "f.jpg", "image/jpeg", JPEG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.id").value(10))
                .andExpect(jsonPath("$.datos.tiene_foto").value(true));

        // Ventana del duplicado = el día de hoy en Paraguay, sin comparar observación (no es "Otro...").
        LocalDateTime hoy = antes.toLocalDate().atStartOfDay();
        verify(dao).existeDuplicado(1, 3, hoy, hoy.plusDays(1), false, null);
    }

    @Test
    void duplicadoDelDiaDa409YNoInserta() throws Exception {
        existenTodos(new AnotacionesDao.Tipo(false, false, false));
        when(dao.existeDuplicado(anyInt(), anyInt(), any(), any(), anyBoolean(), any())).thenReturn(true);

        mockMvc.perform(guardar())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Este alumno ya tiene registrada esa anotación el día de hoy."));
        verify(dao, never()).insertar(anyInt(), anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void tipoRepetibleNoChequeaDuplicado() throws Exception {
        existenTodos(new AnotacionesDao.Tipo(false, false, true));
        mockMvc.perform(guardar()).andExpect(status().isOk());
        verify(dao, never()).existeDuplicado(anyInt(), anyInt(), any(), any(), anyBoolean(), any());
    }

    @Test
    void validacionesDeObservacionYFoto() throws Exception {
        existenTodos(new AnotacionesDao.Tipo(true, true, false));
        mockMvc.perform(guardar())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Este tipo de anotación requiere que escribas la observación"));
        mockMvc.perform(guardar().param("observacion", "x"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Este tipo de anotación requiere una foto de evidencia"));
        mockMvc.perform(guardar().param("observacion", "x")
                        .file(new MockMultipartFile("foto", "f.png", "image/jpeg", new byte[] {(byte) 0x89, 'P', 'N', 'G'})))
                .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(guardar().param("observacion", "x".repeat(501)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("La observación supera los 500 caracteres"));
    }

    private void existenTodos(AnotacionesDao.Tipo tipo) throws Exception {
        when(dao.tipoActivo(3)).thenReturn(tipo);
        when(dao.existeAlumno(1)).thenReturn(true);
        when(dao.existeProfesor(2)).thenReturn(true);
    }

    private MockMultipartHttpServletRequestBuilder guardar() {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/anotaciones/guardar_anotacion");
        req.header("X-API-Key", KEY).param("alumno", "1").param("profesor", "2").param("tipo", "3");
        return req;
    }
}
