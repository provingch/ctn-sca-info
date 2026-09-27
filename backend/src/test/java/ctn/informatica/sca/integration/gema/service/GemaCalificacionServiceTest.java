package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.integration.gema.dao.GemaGradeDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.integration.gema.dto.CalificacionDto;
import ctn.informatica.sca.integration.gema.dto.NotaInputDto;
import ctn.informatica.sca.integration.gema.dto.SaveNotasRequest;
import ctn.informatica.sca.integration.gema.dto.SaveNotasResponse;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GemaCalificacionServiceTest {

    private static Planilla planillaAbierta(int id, int cursoId) {
        return new Planilla(id, cursoId, 20, "especifico", "Programación", 2026, "primera", 10);
    }

    private static Alumno alumno(int id, String ci, int cursoId) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setCi(ci);
        a.setCursoId(cursoId);
        return a;
    }

    private GemaCalificacionService build(PlanillaDao planillaDao, AlumnoDao alumnoDao, TareaDao tareaDao,
            GemaTareaDao gemaTareaDao, GemaGradeDao gemaGradeDao, GradeDao gradeDao, RegistroDao registroDao) {
        return new GemaCalificacionService(planillaDao, alumnoDao, tareaDao, gemaTareaDao, gemaGradeDao, gradeDao, registroDao);
    }

    @Test
    void listCalificacionesMapeaPuntosNulos() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        GemaGradeDao gemaGradeDao = mock(GemaGradeDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, 5));
        when(gemaGradeDao.findGradesForPlanilla(1)).thenReturn(List.of(
                new GemaGradeDao.GradeRow(100, "111", "gema-1", 8),
                new GemaGradeDao.GradeRow(100, "111", "gema-2", null)));

        GemaCalificacionService service = build(planillaDao, mock(AlumnoDao.class), mock(TareaDao.class),
                mock(GemaTareaDao.class), gemaGradeDao, mock(GradeDao.class), mock(RegistroDao.class));

        List<CalificacionDto> result = service.listCalificaciones(1);

        assertEquals(2, result.size());
        assertEquals(8, result.get(0).puntos());
        assertNull(result.get(1).puntos());
    }

    @Test
    void guardaLoteValidoConPuntosNulo() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        GradeDao gradeDao = mock(GradeDao.class);
        RegistroDao registroDao = mock(RegistroDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(100, "111", 5)));
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(tareaDao.findById(50)).thenReturn(new Tarea(50, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea"));
        when(registroDao.getRegistroIdsForPlanilla(1, java.util.Set.of(100))).thenReturn(Map.of(100, 900));

        GemaCalificacionService service = build(planillaDao, alumnoDao, tareaDao, gemaTareaDao, mock(GemaGradeDao.class), gradeDao, registroDao);
        SaveNotasRequest request = new SaveNotasRequest(List.of(new NotaInputDto("111", "gema-1", null)));

        SaveNotasResponse result = service.saveNotas(1, request);

        assertEquals(1, result.recibidas());
        assertEquals(1, result.guardadas());
        assertTrue(result.rechazadas().isEmpty());
        verify(registroDao).ensureRegistroRowsForPlanilla(1, 5);
        Map<Integer, Integer> notaEsperada = new java.util.HashMap<>();
        notaEsperada.put(50, null);
        verify(gradeDao).saveGradesBatch(1, Map.of(900, notaEsperada));
    }

    @Test
    void loteConFilaInvalidaRechazaTodoElLote() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        GradeDao gradeDao = mock(GradeDao.class);
        RegistroDao registroDao = mock(RegistroDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(100, "111", 5)));
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(tareaDao.findById(50)).thenReturn(new Tarea(50, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea"));

        GemaCalificacionService service = build(planillaDao, alumnoDao, tareaDao, gemaTareaDao, mock(GemaGradeDao.class), gradeDao, registroDao);
        SaveNotasRequest request = new SaveNotasRequest(List.of(
                new NotaInputDto("111", "gema-1", 20),
                new NotaInputDto("999", "gema-1", 5)));

        SaveNotasResponse result = service.saveNotas(1, request);

        assertEquals(2, result.recibidas());
        assertEquals(0, result.guardadas());
        assertEquals(1, result.rechazadas().size());
        assertEquals("999", result.rechazadas().get(0).alumnoCi());
        verify(gradeDao, never()).saveGradesBatch(anyInt(), any());
    }

    @Test
    void puntosFueraDeRangoSeRechaza() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        TareaDao tareaDao = mock(TareaDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(100, "111", 5)));
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(new GemaTareaDao.TareaRef(50, 1));
        when(tareaDao.findById(50)).thenReturn(new Tarea(50, 1, 1, LocalDate.of(2026, 3, 1), 20, "Tarea"));

        GemaCalificacionService service = build(planillaDao, alumnoDao, tareaDao, gemaTareaDao, mock(GemaGradeDao.class),
                mock(GradeDao.class), mock(RegistroDao.class));
        SaveNotasRequest request = new SaveNotasRequest(List.of(new NotaInputDto("111", "gema-1", 999)));

        SaveNotasResponse result = service.saveNotas(1, request);

        assertEquals(1, result.rechazadas().size());
        assertTrue(result.rechazadas().get(0).motivo().contains("rango"));
    }

    @Test
    void tareaDeOtraPlanillaSeRechaza() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        AlumnoDao alumnoDao = mock(AlumnoDao.class);
        GemaTareaDao gemaTareaDao = mock(GemaTareaDao.class);
        when(planillaDao.findById(1)).thenReturn(planillaAbierta(1, 5));
        when(alumnoDao.findAllActivos()).thenReturn(List.of(alumno(100, "111", 5)));
        when(gemaTareaDao.findByGemaTareaId("gema-1")).thenReturn(new GemaTareaDao.TareaRef(50, 2));

        GemaCalificacionService service = build(planillaDao, alumnoDao, mock(TareaDao.class), gemaTareaDao,
                mock(GemaGradeDao.class), mock(GradeDao.class), mock(RegistroDao.class));
        SaveNotasRequest request = new SaveNotasRequest(List.of(new NotaInputDto("111", "gema-1", 10)));

        SaveNotasResponse result = service.saveNotas(1, request);

        assertEquals(1, result.rechazadas().size());
        assertTrue(result.rechazadas().get(0).motivo().contains("gemaTareaId"));
    }

    @Test
    void etapaConfirmadaRechaza409() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        Planilla confirmada = planillaAbierta(1, 5);
        confirmada.setEtapa1Confirmada(true);
        when(planillaDao.findById(1)).thenReturn(confirmada);

        GemaCalificacionService service = build(planillaDao, mock(AlumnoDao.class), mock(TareaDao.class),
                mock(GemaTareaDao.class), mock(GemaGradeDao.class), mock(GradeDao.class), mock(RegistroDao.class));
        SaveNotasRequest request = new SaveNotasRequest(List.of(new NotaInputDto("111", "gema-1", 10)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.saveNotas(1, request));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void sinNotasEsBadRequest() {
        GemaCalificacionService service = build(mock(PlanillaDao.class), mock(AlumnoDao.class), mock(TareaDao.class),
                mock(GemaTareaDao.class), mock(GemaGradeDao.class), mock(GradeDao.class), mock(RegistroDao.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveNotas(1, new SaveNotasRequest(List.of())));
        assertEquals(400, ex.getStatusCode().value());
    }
}
