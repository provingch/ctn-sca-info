package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.LoginRequest;
import ctn.informatica.sca.dto.LoginResponse;
import ctn.informatica.sca.dto.Verify2faRequest;
import ctn.informatica.sca.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (AuthService.AuthException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(@RequestBody Verify2faRequest req) {
        try {
            return ResponseEntity.ok(authService.verify2fa(req));
        } catch (AuthService.AuthException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}