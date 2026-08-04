package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.AssignFaltaCodigoRequest;
import ctn.informatica.sca.dto.CreateRasgoPlanillaRequest;
import ctn.informatica.sca.dto.HomeResponse;
import ctn.informatica.sca.dto.SubmitRasgoAsistenciaRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public HomeResponse getHome(
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer etapa,
            @RequestParam(required = false) String view,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/create-rasgo-planilla")
    @PreAuthorize("hasRole('LEVEL_1')")
    public void createRasgoPlanilla(
            @RequestBody CreateRasgoPlanillaRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/submit-rasgo-asistencia")
    @PreAuthorize("hasRole('LEVEL_1')")
    public void submitRasgoAsistencia(
            @RequestBody SubmitRasgoAsistenciaRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/assign-falta-codigo")
    @PreAuthorize("hasRole('LEVEL_1')")
    public void assignFaltaCodigo(
            @RequestBody AssignFaltaCodigoRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
