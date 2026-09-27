package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.MateriaDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaMateriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaMateriaController {

    private final GemaMateriaService materiaService;

    public GemaMateriaController(GemaMateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @GetMapping("/materias")
    public PageResponse<MateriaDto> materias(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(materiaService.listMaterias(), page, size);
    }
}
