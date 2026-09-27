package ctn.informatica.sca.integration.gema.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Profesor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cierre de Fase 1 contra el contexto real de Spring (mismo enfoque que
 * {@code GemaPingIntegrationTest}): catálogos de solo lectura autenticados por {@code X-API-Key},
 * con los DAOs reales mockeados (evita depender de una base disponible en este entorno de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gema.integration.api-key=clave-de-test-de-integracion")
class GemaCatalogIntegrationTest {

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
    private CursoDao cursoDao;

    @MockitoBean
    private MateriaDao materiaDao;

    @MockitoBean
    private ProfesorDao profesorDao;

    @MockitoBean
    private AlumnoDao alumnoDao;

    @MockitoBean
    private AsignacionDao asignacionDao;

    private static Profesor profesorSensible() {
        Profesor p = new Profesor();
        p.setId(1);
        p.setCi(555);
        p.setNombre("Ana");
        p.setApellido("Pérez");
        p.setNivel(1);
        p.setContrasenia("$2a$10$hashSecretoQueNuncaDeberiaSalir");
        p.setTotpSecret("secreto-totp-no-exponer");
        p.setGcAccessToken("token-google-no-exponer");
        return p;
    }

    @Test
    void cursosDevuelveEnvoltorioDePaginacion() throws Exception {
        when(cursoDao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(new Curso(1, "Informática", 2027, "A"))));

        mockMvc.perform(get("/api/integracion/gema/cursos").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.data[0].especialidad").value("Informática"));
    }

    @Test
    void materiasDevuelveCategoriaYEspecialidadIds() throws Exception {
        when(materiaDao.listAll()).thenReturn(List.of(new Materia(1, "Matemática", "comun")));
        when(materiaDao.listEspecialidadIdsForMateria(1)).thenReturn(List.of(9));

        mockMvc.perform(get("/api/integracion/gema/materias").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].especialidadIds[0]").value(9));
    }

    @Test
    void docentesNuncaExponeCamposSensibles() throws Exception {
        when(profesorDao.findAll()).thenReturn(List.of(profesorSensible()));

        mockMvc.perform(get("/api/integracion/gema/docentes").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ci").value(555))
                .andExpect(content().string(not(containsString("hashSecreto"))))
                .andExpect(content().string(not(containsString("totp"))))
                .andExpect(content().string(not(containsString("token-google"))));
    }

    @Test
    void docentePorCiInexistenteDa404() throws Exception {
        when(profesorDao.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/integracion/gema/docentes/12345").header("X-API-Key", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void alumnosToleraCiNulo() throws Exception {
        Alumno sinCi = new Alumno();
        sinCi.setId(1);
        sinCi.setCi(null);
        sinCi.setNombre("Juan");
        sinCi.setApellido("Gómez");
        sinCi.setCursoId(5);
        when(alumnoDao.findAllActivos()).thenReturn(List.of(sinCi));
        when(cursoDao.findAll()).thenReturn(new java.util.ArrayList<>(List.of(new Curso(5, "Informática", 2027, "A"))));

        mockMvc.perform(get("/api/integracion/gema/alumnos").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ci").doesNotExist());
    }

    @Test
    void asignacionesIncluyeReferenciasResueltas() throws Exception {
        Asignacion a = new Asignacion(1, 1, 20, 0);
        a.setMateriaNombre("Programación");
        a.setEspecialidad("Informática");
        a.setCursoNivel(3);
        a.setCursoSeccion("A");
        when(asignacionDao.findAll()).thenReturn(List.of(a));
        when(profesorDao.findAll()).thenReturn(List.of(profesorSensible()));

        mockMvc.perform(get("/api/integracion/gema/asignaciones").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].docente.ci").value(555))
                .andExpect(jsonPath("$.data[0].materia.nombre").value("Programación"));
    }

    @Test
    void sinClaveTodosLosCatalogosRespondenNoAutorizado() throws Exception {
        for (String path : List.of("/cursos", "/materias", "/docentes", "/alumnos", "/asignaciones")) {
            mockMvc.perform(get("/api/integracion/gema" + path)).andExpect(status().isUnauthorized());
        }
    }
}
