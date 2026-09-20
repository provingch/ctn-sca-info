package ctn.informatica.sca.controller;

import ctn.informatica.sca.controller.PlanillaController.PlanillaDetailResponse;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.service.PlanillaReaperturaService;
import ctn.informatica.sca.service.PlanillaReaperturaService.ReaperturaRequest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Vista de sólo lectura de las planillas para evaluación, más la reapertura de etapas con motivo. A diferencia de
 * {@code /api/planillas} (del profesor, que exige ser el dueño) acá no hay ownership: evaluación ve todas.
 */
@RestController
@RequestMapping("/api/evaluacion")
@PreAuthorize("hasRole('LEVEL_2')")
public class EvaluacionPlanillaController {

    public record PlanillaResumenDto(
            int id,
            int cursoId,
            int materiaId,
            String materiaNombre,
            int profesorId,
            String profesorNombre,
            int etapaIndex,
            int periodo,
            LocalDate fechaCierreEtapa1,
            boolean etapa1Confirmada,
            LocalDate fechaCierreEtapa2,
            boolean etapa2Confirmada) {
    }

    private final PlanillaDao planillaDao;
    private final CursoDao cursoDao;
    private final EspecialidadDao especialidadDao;
    private final ProfesorDao profesorDao;
    private final PlanillaReaperturaService reaperturaService;

    @Autowired
    public EvaluacionPlanillaController(PlanillaDao planillaDao, CursoDao cursoDao, EspecialidadDao especialidadDao,
            ProfesorDao profesorDao, PlanillaReaperturaService reaperturaService) {
        this.planillaDao = planillaDao;
        this.cursoDao = cursoDao;
        this.especialidadDao = especialidadDao;
        this.profesorDao = profesorDao;
        this.reaperturaService = reaperturaService;
    }

    @GetMapping("/planillas")
    public List<PlanillaResumenDto> listarPlanillas(
            @RequestParam int cursoId,
            @RequestParam String etapa,
            @RequestParam int periodo,
            @RequestParam(required = false) Integer materiaId,
            Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<PlanillaResumenDto> result = new ArrayList<>();
            for (EvaluacionPlanillaSelector.Match match : EvaluacionPlanillaSelector.select(
                    planillaDao, cursoDao, especialidadDao, cursoId, etapa, periodo, materiaId).matches()) {
                Planilla planilla = match.planilla();
                Profesor profesor = profesorDao.findById(planilla.getProfesorId());
                result.add(new PlanillaResumenDto(
                        planilla.getId(),
                        planilla.getCursoId(),
                        planilla.getMateriaId(),
                        match.materiaNombre(),
                        planilla.getProfesorId(),
                        profesor == null ? "" : profesor.getFullName(),
                        planilla.getEtapaIndex(),
                        planilla.getPeriodo(),
                        planilla.getFechaCierreEtapa1(),
                        planilla.getEtapa1Confirmada(),
                        planilla.getFechaCierreEtapa2(),
                        planilla.getEtapa2Confirmada()));
            }
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron listar las planillas", ex);
        }
    }

    @GetMapping("/planillas/{id}")
    public PlanillaDetailResponse verPlanilla(@PathVariable int id, Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = planillaDao.findById(id);
            if (planilla == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
            }
            // Sólo lectura: no se crean filas de registro faltantes como cuando la abre el profesor.
            return PlanillaDetailAssembler.build(planilla, false);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar planilla", ex);
        }
    }

    @PostMapping("/planillas/{id}/etapa1/reabrir")
    public Map<String, Object> reabrirEtapa1(@PathVariable int id, @RequestBody(required = false) ReaperturaRequest body, Authentication authentication) {
        return reabrir(id, 1, body, authentication);
    }

    @PostMapping("/planillas/{id}/etapa2/reabrir")
    public Map<String, Object> reabrirEtapa2(@PathVariable int id, @RequestBody(required = false) ReaperturaRequest body, Authentication authentication) {
        return reabrir(id, 2, body, authentication);
    }

    private Map<String, Object> reabrir(int id, int etapa, ReaperturaRequest body, Authentication authentication) {
        int actorId = ApiAuth.requireUserId(authentication);
        try {
            reaperturaService.reabrirEtapa(id, etapa, actorId, body == null ? null : body.motivo());
            return Map.of("planillaId", id, "etapa" + etapa + "Confirmada", false);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo reabrir Etapa " + etapa, ex);
        }
    }
}
