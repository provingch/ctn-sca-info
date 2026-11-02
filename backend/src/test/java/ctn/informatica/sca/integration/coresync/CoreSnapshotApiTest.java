package ctn.informatica.sca.integration.coresync;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sca.core-sync.api-key=clave-sync-test")
class CoreSnapshotApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseMigrationInitializer databaseMigrationInitializer;

    @MockitoBean
    private RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;

    @MockitoBean
    private CoreSnapshotDao dao;

    @Test
    void requiereClaveDeSincronizacionAntesDeExportar() throws Exception {
        mockMvc.perform(get("/api/internal/core/snapshot"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/internal/core/snapshot").header("X-Core-Sync-Key", "otra-clave"))
                .andExpect(status().isUnauthorized());
        verify(dao, never()).exportar();
    }

    @Test
    void exportaSoloElPerfilInstitucionalSinCredenciales() throws Exception {
        CoreSnapshot snapshot = new CoreSnapshot(
                1, LocalDateTime.parse("2026-11-02T13:00:00"),
                List.of(new CoreSnapshot.Especialidad(1, "Informática")),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CoreSnapshot.Perfil(7, "Ana", "Docente", 1234567, null, null,
                        "ana@ctn.edu.py", 1, 1)),
                List.of(), List.of(), List.of(), List.of(), List.of());
        when(dao.exportar()).thenReturn(snapshot);

        mockMvc.perform(get("/api/internal/core/snapshot").header("X-Core-Sync-Key", "clave-sync-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.perfiles[0].id").value(7))
                .andExpect(jsonPath("$.perfiles[0].contrasenia").doesNotExist())
                .andExpect(jsonPath("$.perfiles[0].googleRefreshToken").doesNotExist());
        verify(dao).exportar();
    }
}
