package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.integration.gema.dto.CreateTareaRequest;
import ctn.informatica.sca.integration.gema.dto.CreateTareaResult;
import ctn.informatica.sca.integration.gema.dto.TareaDto;
import ctn.informatica.sca.integration.gema.dto.UpdateTareaRequest;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GemaTareaServiceTest {

    private static Planilla planillaAbierta(int id, String etapa, LocalDate fechaCierreEtapa1) {
        Planilla p = new Planilla(id, 5, 20, "especifico", "Programación", 2026, etapa, 10);
        p.setFechaCierreEtapa1(fechaCierreEtapa1);
        return p;
    }

    private static Tarea tarea(int id, int planillaId) {
        return new Tarea(id, planillaId, 1, LocalDate.of(2026, 3, 10), 20, "Tarea vieja");
    }

    @Test
    void listTareasClasificaOrigen() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, "primera", null));
        when(instrumentoDao.findAll()).thenReturn(List.of(new Instrumento(1, "Prueba escrita")));

        Tarea deClassroom = tarea(1, 1);
        deClassroom.setGoogleCourseworkId("gc-1");
        Tarea deGema = tarea(2, 1);
        Tarea deSca = tarea(3, 1);
        when(tareaDao.consultarTarea(1)).thenReturn(new java.util.ArrayList<>(List.of(deClassroom, deGema, deSca)));
        when(gemaTareaDao.findGemaTareaIdsByPlanilla(1)).thenReturn(java.util.Map.of(2, "gema-abc"));

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        List<TareaDto> result = service.listTareas(1);

        assertEquals("CLASSROOM", result.get(0).origen());
        assertEquals("GEMA", result.get(1).origen());
        assertEquals("SCA", result.get(2).origen());
        assertEquals("Prueba escrita", result.get(0).instrumentoNombre());
    }

    @Test
    void createTareaNuevaInsertaYDevuelve201() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, "primera", null));
        when(instrumentoDao.findAll()).thenReturn(List.of());
        when(gemaTareaDao.findByGemaTareaId("gema-125")).thenReturn(null);
        when(gemaTareaDao.insert(1, 1, LocalDate.of(2026, 3, 1), null, LocalDate.of(2026, 3, 10), 20, "Tarea nueva", "gema-125"))
                .thenReturn(99);
        when(tareaDao.findById(99)).thenReturn(new Tarea(99, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea nueva"));

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        CreateTareaRequest request = new CreateTareaRequest("gema-125", "Tarea nueva",
                LocalDate.of(2026, 3, 1), null, LocalDate.of(2026, 3, 10), 20, 1);

        CreateTareaResult result = service.createTarea(1, request);

        assertTrue(result.created());
        assertEquals(99, result.tarea().id());
        assertEquals("GEMA", result.tarea().origen());
    }

    @Test
    void createTareaReenviadaEsIdempotenteYActualiza() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, "primera", null));
        when(instrumentoDao.findAll()).thenReturn(List.of());
        when(gemaTareaDao.findByGemaTareaId("gema-125")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(tareaDao.findById(50)).thenReturn(tarea(50, 1));

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        CreateTareaRequest request = new CreateTareaRequest("gema-125", "Tarea actualizada",
                LocalDate.of(2026, 3, 1), null, LocalDate.of(2026, 3, 10), 30, 1);

        CreateTareaResult result = service.createTarea(1, request);

        assertFalse(result.created());
        verify(tareaDao, times(1)).update(any());
        verify(gemaTareaDao, never()).insert(anyInt(), anyInt(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void createTareaConGemaTareaIdDeOtraPlanillaEsConflicto() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, "primera", null));
        when(gemaTareaDao.findByGemaTareaId("gema-125")).thenReturn(new GemaTareaDao.TareaRef(50, 2));

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        CreateTareaRequest request = new CreateTareaRequest("gema-125", "Tarea", LocalDate.of(2026, 3, 1), null, null, 20, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createTarea(1, request));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void createTareaConEtapaConfirmadaRechaza409() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        Planilla confirmada = planillaAbierta(1, "primera", null);
        confirmada.setEtapa1Confirmada(true);
        when(planillaDao.findById(1)).thenReturn(confirmada);

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        CreateTareaRequest request = new CreateTareaRequest("gema-1", "Tarea", LocalDate.of(2026, 3, 1), null, null, 20, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createTarea(1, request));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void createTareaConFechaFueraDeEtapaRechaza409() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        // etapa "primera" pero la fecha límite cae después del cierre de etapa 1: pertenece a la segunda.
        Planilla primera = planillaAbierta(1, "primera", LocalDate.of(2026, 6, 1));
        when(planillaDao.findById(1)).thenReturn(primera);

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        CreateTareaRequest request = new CreateTareaRequest("gema-1", "Tarea",
                LocalDate.of(2026, 3, 1), null, LocalDate.of(2026, 7, 5), 20, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createTarea(1, request));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void updateTareaSinExistenciaDa404() {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        UpdateTareaRequest request = new UpdateTareaRequest("Tarea", LocalDate.of(2026, 3, 1), null, null, 20, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.updateTarea(1, "gema-x", request));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateTareaExistenteActualizaPreservandoGemaTareaId() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        InstrumentoDao instrumentoDao = mock(InstrumentoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(gemaTareaDao.findByGemaTareaId("gema-125")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, "primera", null));
        when(tareaDao.findById(50)).thenReturn(tarea(50, 1));
        when(instrumentoDao.findAll()).thenReturn(List.of());

        GemaTareaService service = new GemaTareaService(planillaDao, tareaDao, instrumentoDao, gemaTareaDao);
        UpdateTareaRequest request = new UpdateTareaRequest("Título nuevo", LocalDate.of(2026, 3, 1), null, null, 25, 1);

        TareaDto result = service.updateTarea(1, "gema-125", request);

        assertEquals("Título nuevo", result.titulo());
        assertEquals(25, result.total());
        verify(tareaDao).update(any());
    }
}
