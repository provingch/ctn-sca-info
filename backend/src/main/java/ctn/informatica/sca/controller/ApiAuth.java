package ctn.informatica.sca.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

final class ApiAuth {

    private ApiAuth() {
    }

    static int requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value.intValue();
        }
        if (principal instanceof Integer value) {
            return value;
        }
        if (principal instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
    }

    static int requireUserLevel(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_LEVEL_"))
                .map(authority -> authority.substring("ROLE_LEVEL_".length()))
                .map(level -> {
                    try {
                        return Integer.parseInt(level);
                    } catch (NumberFormatException ex) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
                    }
                })
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token sin rol de usuario"));
    }
}
