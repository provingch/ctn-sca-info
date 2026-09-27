package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.CursoDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaCursoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaCursoController {

    private final GemaCursoService cursoService;

    public GemaCursoController(GemaCursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping("/cursos")
    public PageResponse<CursoDto> cursos(
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(cursoService.listCursos(especialidad), page, size);
    }
}
