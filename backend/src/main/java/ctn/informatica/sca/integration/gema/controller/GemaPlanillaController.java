package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.CalificacionDto;
import ctn.informatica.sca.integration.gema.dto.CreateTareaRequest;
import ctn.informatica.sca.integration.gema.dto.CreateTareaResult;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.dto.PlanillaDto;
import ctn.informatica.sca.integration.gema.dto.SaveNotasRequest;
import ctn.informatica.sca.integration.gema.dto.SaveNotasResponse;
import ctn.informatica.sca.integration.gema.dto.TareaDto;
import ctn.informatica.sca.integration.gema.dto.UpdateTareaRequest;
import ctn.informatica.sca.integration.gema.service.GemaCalificacionService;
import ctn.informatica.sca.integration.gema.service.GemaPlanillaService;
import ctn.informatica.sca.integration.gema.service.GemaTareaService;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaPlanillaController {

    private final GemaPlanillaService planillaService;
    private final GemaTareaService tareaService;
    private final GemaCalificacionService calificacionService;

    public GemaPlanillaController(GemaPlanillaService planillaService, GemaTareaService tareaService,
            GemaCalificacionService calificacionService) {
        this.planillaService = planillaService;
        this.tareaService = tareaService;
        this.calificacionService = calificacionService;
    }

    @GetMapping("/planillas")
    public PageResponse<PlanillaDto> planillas(
            @RequestParam(required = false) Integer docenteCi,
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(planillaService.listPlanillas(docenteCi, cursoId, materiaId, periodo, fecha), page, size);
    }

    @GetMapping("/planillas/{planillaId}/tareas")
    public PageResponse<TareaDto> tareas(
            @PathVariable int planillaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(tareaService.listTareas(planillaId), page, size);
    }

    @PostMapping("/planillas/{planillaId}/tareas")
    public ResponseEntity<TareaDto> crearTarea(@PathVariable int planillaId, @RequestBody CreateTareaRequest request) {
        CreateTareaResult result = tareaService.createTarea(planillaId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.tarea());
    }

    @PutMapping("/planillas/{planillaId}/tareas/{gemaTareaId}")
    public TareaDto actualizarTarea(
            @PathVariable int planillaId,
            @PathVariable String gemaTareaId,
            @RequestBody UpdateTareaRequest request) {
        return tareaService.updateTarea(planillaId, gemaTareaId, request);
    }

    @GetMapping("/planillas/{planillaId}/calificaciones")
    public PageResponse<CalificacionDto> calificaciones(
            @PathVariable int planillaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(calificacionService.listCalificaciones(planillaId), page, size);
    }

    @PutMapping("/planillas/{planillaId}/calificaciones")
    public ResponseEntity<SaveNotasResponse> guardarCalificaciones(
            @PathVariable int planillaId,
            @RequestBody SaveNotasRequest request) {
        SaveNotasResponse result = calificacionService.saveNotas(planillaId, request);
        HttpStatus status = result.rechazadas().isEmpty() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(result);
    }
}
