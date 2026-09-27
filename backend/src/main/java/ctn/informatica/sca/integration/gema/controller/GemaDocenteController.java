package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.DocenteDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaDocenteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaDocenteController {

    private final GemaDocenteService docenteService;

    public GemaDocenteController(GemaDocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping("/docentes")
    public PageResponse<DocenteDto> docentes(
            @RequestParam(required = false, defaultValue = "false") boolean incluirEvaluadores,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(docenteService.listDocentes(incluirEvaluadores), page, size);
    }

    @GetMapping("/docentes/{ci}")
    public DocenteDto docente(
            @PathVariable String ci,
            @RequestParam(required = false, defaultValue = "false") boolean incluirEvaluadores) {
        DocenteDto docente = docenteService.findByCi(parseCi(ci), incluirEvaluadores);
        if (docente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un docente con esa cédula");
        }
        return docente;
    }

    private static int parseCi(String ci) {
        try {
            return Integer.parseInt(ci.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula debe ser numérica");
        }
    }
}
