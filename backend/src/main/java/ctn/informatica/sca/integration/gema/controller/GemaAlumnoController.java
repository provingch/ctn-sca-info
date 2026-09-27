package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.AlumnoDto;
import ctn.informatica.sca.integration.gema.dto.PageResponse;
import ctn.informatica.sca.integration.gema.service.GemaAlumnoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/integracion/gema")
public class GemaAlumnoController {

    private final GemaAlumnoService alumnoService;

    public GemaAlumnoController(GemaAlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @GetMapping("/alumnos")
    public PageResponse<AlumnoDto> alumnos(
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) String ci,
            @RequestParam(required = false, defaultValue = "false") boolean egresados,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(alumnoService.listAlumnos(cursoId, ci, egresados), page, size);
    }

    @GetMapping("/alumnos/{ci}")
    public AlumnoDto alumno(
            @PathVariable String ci,
            @RequestParam(required = false, defaultValue = "false") boolean egresados) {
        AlumnoDto alumno = alumnoService.findByCi(ci, egresados);
        if (alumno == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un alumno con esa cédula");
        }
        return alumno;
    }
}
