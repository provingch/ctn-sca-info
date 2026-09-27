package ctn.informatica.sca.integration.gema.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cierre de Fase 2 contra el contexto real de Spring, mismo enfoque que
 * {@code GemaCatalogIntegrationTest}: DAOs reales mockeados, sin depender de una base disponible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gema.integration.api-key=clave-de-test-de-integracion")
class GemaPlanillaIntegrationTest {

    private static final String API_KEY = "clave-de-test-de-integracion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseMigrationInitializer databaseMigrationInitializer;
    @MockitoBean
    private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;
    @MockitoBean
    private UserDao userDao;
    @MockitoBean
    private PlanillaDao planillaDao;
    @MockitoBean
    private TareaDao tareaDao;
    @MockitoBean
    private InstrumentoDao instrumentoDao;
    @MockitoBean
    private GemaTareaDao gemaTareaDao;
    @MockitoBean
    private ProfesorDao profesorDao;

    private static Planilla planilla(int id, String etapa) {
        return new Planilla(id, 5, 20, "especifico", "Programación", ctn.informatica.sca.util.AcademicPeriod.current(), etapa, 10);
    }

    @Test
    void planillasDevuelveEnvoltorio() throws Exception {
        when(planillaDao.findByCompositeKey(5, 20, 1)).thenReturn(planilla(1, "primera"));
        when(planillaDao.findByCompositeKey(5, 20, 2)).thenReturn(null);

        mockMvc.perform(get("/api/integracion/gema/planillas?cursoId=5&materiaId=20").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].etapa").value("primera"));
    }

    @Test
    void planillasSinCursoIdEsBadRequest() throws Exception {
        mockMvc.perform(get("/api/integracion/gema/planillas?materiaId=20").header("X-API-Key", API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tareasDePlanillaDevuelveEnvoltorio() throws Exception {
        when(planillaDao.findById(1)).thenReturn(planilla(1, "primera"));
        when(tareaDao.consultarTarea(1)).thenReturn(new java.util.ArrayList<>(
                List.of(new Tarea(9, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea X"))));
        when(instrumentoDao.findAll()).thenReturn(List.of(new Instrumento(1, "Prueba")));
        when(gemaTareaDao.findGemaTareaIdsByPlanilla(1)).thenReturn(java.util.Map.of());

        mockMvc.perform(get("/api/integracion/gema/planillas/1/tareas").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].origen").value("SCA"));
    }

    @Test
    void crearTareaNuevaDevuelve201() throws Exception {
        when(planillaDao.findById(1)).thenReturn(planilla(1, "primera"));
        when(instrumentoDao.findAll()).thenReturn(List.of());
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(null);
        when(gemaTareaDao.insert(1, 1, LocalDate.of(2026, 3, 1), null, null, 20, "Tarea nueva", "gema-1")).thenReturn(77);
        when(tareaDao.findById(77)).thenReturn(new Tarea(77, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea nueva"));

        String body = "{\"gemaTareaId\":\"gema-1\",\"titulo\":\"Tarea nueva\",\"fecha\":\"2026-03-01\",\"total\":20,\"instrumentoId\":1}";
        mockMvc.perform(post("/api/integracion/gema/planillas/1/tareas")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("GEMA"));
    }

    @Test
    void actualizarTareaInexistenteDevuelve404() throws Exception {
        when(gemaTareaDao.findByGemaTareaId("gema-99")).thenReturn(null);

        String body = "{\"titulo\":\"Tarea\",\"fecha\":\"2026-03-01\",\"total\":20,\"instrumentoId\":1}";
        mockMvc.perform(put("/api/integracion/gema/planillas/1/tareas/gema-99")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void instrumentosDevuelveEnvoltorio() throws Exception {
        when(instrumentoDao.findAll()).thenReturn(List.of(new Instrumento(1, "Prueba escrita")));

        mockMvc.perform(get("/api/integracion/gema/instrumentos").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Prueba escrita"));
    }
}
