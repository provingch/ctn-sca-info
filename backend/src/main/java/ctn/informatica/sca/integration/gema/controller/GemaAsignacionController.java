package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.AsignacionDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaAsignacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaAsignacionController {

    private final GemaAsignacionService asignacionService;

    public GemaAsignacionController(GemaAsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @GetMapping("/asignaciones")
    public PageResponse<AsignacionDto> asignaciones(
            @RequestParam(required = false) Integer docenteCi,
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(asignacionService.listAsignaciones(docenteCi, cursoId), page, size);
    }
}
