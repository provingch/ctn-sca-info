package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.InstrumentoDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaTareaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaInstrumentoController {

    private final GemaTareaService tareaService;

    public GemaInstrumentoController(GemaTareaService tareaService) {
        this.tareaService = tareaService;
    }

    @GetMapping("/instrumentos")
    public PageResponse<InstrumentoDto> instrumentos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(tareaService.listInstrumentos(), page, size);
    }
}
