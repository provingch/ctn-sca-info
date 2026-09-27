package ctn.informatica.sca.integration.gema.controller;

import ctn.informatica.sca.integration.gema.dto.PingResponse;
import ctn.informatica.sca.integration.gema.service.GemaPingService;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fase 0: prueba básica de conexión para GEMA. Autenticado por {@code X-API-Key} (ver paquete {@code security}). */
@RestController
@RequestMapping("/api/integracion/gema")
public class GemaPingController {

    private final GemaPingService pingService;

    public GemaPingController(GemaPingService pingService) {
        this.pingService = pingService;
    }

    @GetMapping("/ping")
    public ResponseEntity<PingResponse> ping(Authentication authentication) {
        boolean databaseUp = pingService.isDatabaseUp();
        PingResponse body = new PingResponse(
                "ok",
                "sca-backend",
                authentication.getName(),
                OffsetDateTime.now().toString(),
                databaseUp ? "ok" : "error",
                pingService.jarVersion());
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
