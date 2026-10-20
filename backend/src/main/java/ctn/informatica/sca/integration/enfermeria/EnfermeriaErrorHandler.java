package ctn.informatica.sca.integration.enfermeria;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Errores de {@link EnfermeriaController} en el formato {@code {ok: false, error}} que espera la
 * app, en lugar del {@code {status, error, message}} global de {@code ApiExceptionHandler}.
 * Acotado a ese controlador y con precedencia máxima para ganarle al handler global.
 */
@RestControllerAdvice(assignableTypes = EnfermeriaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnfermeriaErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(EnfermeriaErrorHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> conEstado(ResponseStatusException ex) {
        return error(ex.getStatusCode().value(), ex.getReason());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> faltaParametro(MissingServletRequestParameterException ex) {
        return error(422, "Falta el parámetro '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(422, "El parámetro '" + ex.getName() + "' debe ser un número entero");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> interno(Exception ex) {
        log.error("Error en la API de enfermería", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor");
    }

    static ResponseEntity<Map<String, Object>> error(int status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("ok", false, "error", mensaje));
    }
}
