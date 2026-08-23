package ctn.informatica.sca.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public static record ApiError(int status, String error, String message) {}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex) {
        // Log full stacktrace to server logs, but do not expose it to clients
        log.error("Unhandled exception while processing request", ex);
        ApiError body = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "Ocurrió un error al procesar la solicitud");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        // Expected client errors (400/404/etc). Log as warn and preserve message.
        String reason = ex.getReason();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("Request rejected: {} - {}", status, reason != null ? reason : "");
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), reason != null ? reason : status.getReasonPhrase());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        log.warn("Static resource not found: {}", path);
        ApiError body = new ApiError(HttpStatus.NOT_FOUND.value(), "Not Found", "Recurso no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
