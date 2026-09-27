package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.PlanillaDto;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GemaPlanillaServiceTest {

    private static Planilla planilla(int id, String etapa, int profesorId, LocalDate fechaCierreEtapa1) {
        Planilla p = new Planilla(id, 5, 20, "especifico", "Programación", AcademicPeriod.current(), etapa, profesorId);
        p.setFechaCierreEtapa1(fechaCierreEtapa1);
        return p;
    }

    private static Profesor profesor(int id, int ci) {
        Profesor p = new Profesor();
        p.setId(id);
        p.setCi(ci);
        return p;
    }

    @Test
    void sinCursoOMateriaRespondeBadRequest() {
        GemaPlanillaService service = new GemaPlanillaService(mock(PlanillaDao.class), mock(ProfesorDao.class));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listPlanillas(null, null, 20, null, null));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void sinFechaDevuelveLasDosEtapasQueExistan() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        when(planillaDao.findByCompositeKey(5, 20, 1)).thenReturn(planilla(1, "primera", 10, LocalDate.of(2026, 6, 1)));
        when(planillaDao.findByCompositeKey(5, 20, 2)).thenReturn(planilla(2, "segunda", 10, LocalDate.of(2026, 6, 1)));
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555)));
        GemaPlanillaService service = new GemaPlanillaService(planillaDao, profesorDao);

        List<PlanillaDto> result = service.listPlanillas(null, 5, 20, null, null);

        assertEquals(2, result.size());
        assertEquals(555, result.get(0).docenteCi());
    }

    @Test
    void conFechaResuelveUnaSolaEtapaSegunFechaCierreEtapa1() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        LocalDate cierreEtapa1 = LocalDate.of(2026, 6, 1);
        when(planillaDao.findByCompositeKey(5, 20, 1)).thenReturn(planilla(1, "primera", 10, cierreEtapa1));
        when(planillaDao.findByCompositeKey(5, 20, 2)).thenReturn(planilla(2, "segunda", 10, cierreEtapa1));
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555)));
        GemaPlanillaService service = new GemaPlanillaService(planillaDao, profesorDao);

        List<PlanillaDto> despuesDelCierre = service.listPlanillas(null, 5, 20, null, LocalDate.of(2026, 7, 5));
        List<PlanillaDto> antesDelCierre = service.listPlanillas(null, 5, 20, null, LocalDate.of(2026, 3, 1));

        assertEquals(1, despuesDelCierre.size());
        assertEquals("segunda", despuesDelCierre.get(0).etapa());
        assertEquals(1, antesDelCierre.size());
        assertEquals("primera", antesDelCierre.get(0).etapa());
    }

    @Test
    void filtraPorDocenteCiSinCoincidenciaDevuelveVacio() throws SQLException {
        PlanillaDao planillaDao = mock(PlanillaDao.class);
        ProfesorDao profesorDao = mock(ProfesorDao.class);
        when(profesorDao.findAll()).thenReturn(List.of(profesor(10, 555)));
        GemaPlanillaService service = new GemaPlanillaService(planillaDao, profesorDao);

        List<PlanillaDto> result = service.listPlanillas(999, 5, 20, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void periodoDistintoAlActualDevuelveVacio() {
        GemaPlanillaService service = new GemaPlanillaService(mock(PlanillaDao.class), mock(ProfesorDao.class));

        List<PlanillaDto> result = service.listPlanillas(null, 5, 20, AcademicPeriod.current() - 5, null);

        assertTrue(result.isEmpty());
    }
}
