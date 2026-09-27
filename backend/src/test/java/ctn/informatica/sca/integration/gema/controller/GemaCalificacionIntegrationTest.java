package ctn.informatica.sca.integration.gema.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.integration.gema.dao.GemaGradeDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.model.Alumno;
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

/** Cierre de Fase 3, mismo enfoque que las integraciones anteriores: DAOs reales mockeados. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gema.integration.api-key=clave-de-test-de-integracion")
class GemaCalificacionIntegrationTest {

    private static final String API_KEY = "clave-de-test-de-integracion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DatabaseMigrationInitializer databaseMigrationInitializer;
    @MockitoBean private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;
    @MockitoBean private UserDao userDao;
    @MockitoBean private PlanillaDao planillaDao;
    @MockitoBean private AlumnoDao alumnoDao;
    @MockitoBean private TareaDao tareaDao;
    @MockitoBean private InstrumentoDao instrumentoDao;
    @MockitoBean private GemaTareaDao gemaTareaDao;
    @MockitoBean private GemaGradeDao gemaGradeDao;
    @MockitoBean private GradeDao gradeDao;
    @MockitoBean private RegistroDao registroDao;
    @MockitoBean private ProfesorDao profesorDao;

    private static Planilla planilla(int id, int cursoId) {
        return new Planilla(id, cursoId, 20, "especifico", "Programación", ctn.informatica.sca.util.AcademicPeriod.current(), "primera", 10);
    }

    private static Alumno alumno(int id, String ci, int cursoId) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setCi(ci);
        a.setCursoId(cursoId);
        return a;
    }

    @Test
    void calificacionesDevuelvePuntosNuloComoNull() throws Exception {
        when(planillaDao.findById(1)).thenReturn(planilla(1, 5));
        when(gemaGradeDao.findGradesForPlanilla(1)).thenReturn(List.of(
                new GemaGradeDao.GradeRow(100, "111", "gema-1", null)));

        mockMvc.perform(get("/api/integracion/gema/planillas/1/calificaciones").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].puntos").doesNotExist());
    }

    @Test
    void guardarCalificacionesLoteValidoDevuelve200() throws Exception {
        when(planillaDao.findById(1)).thenReturn(planilla(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(100, "111", 5)));
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(tareaDao.findById(50)).thenReturn(new Tarea(50, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea"));
        when(registroDao.getRegistroIdsForPlanilla(1, java.util.Set.of(100))).thenReturn(java.util.Map.of(100, 900));

        String body = "{\"notas\":[{\"alumnoCi\":\"111\",\"gemaTareaId\":\"gema-1\",\"puntos\":15}]}";
        mockMvc.perform(put("/api/integracion/gema/planillas/1/calificaciones")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardadas").value(1))
                .andExpect(jsonPath("$.rechazadas").isEmpty());
    }

    @Test
    void guardarCalificacionesConFilaInvalidaDevuelve400ConMotivo() throws Exception {
        when(planillaDao.findById(1)).thenReturn(planilla(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of());

        String body = "{\"notas\":[{\"alumnoCi\":\"999\",\"gemaTareaId\":\"gema-1\",\"puntos\":15}]}";
        mockMvc.perform(put("/api/integracion/gema/planillas/1/calificaciones")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.guardadas").value(0))
                .andExpect(jsonPath("$.rechazadas[0].alumnoCi").value("999"));
    }
}
