package ctn.informatica.sca.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
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

    /**
     * Rol sin permiso (p. ej. {@code @PreAuthorize}): es un 403, no un 500. Sin esto caía en
     * {@link #handleAll} y cada intento quedaba como ERROR en el log, tapando los errores reales.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Request rejected: 403 FORBIDDEN - {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "Forbidden", "No tenés permiso para realizar esta acción"));
    }

    /**
     * Errores del pedido que Spring MVC ya clasifica con su código (parámetro faltante o de tipo
     * inválido, cuerpo ilegible, método no soportado...): se responden con ese 4xx.
     * {@link ResponseStatusException} y {@link NoResourceFoundException} tienen su propio handler.
     */
    @ExceptionHandler({org.springframework.web.bind.ServletRequestBindingException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.HttpRequestMethodNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.multipart.MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        HttpStatus status = ex instanceof ErrorResponse er ? HttpStatus.valueOf(er.getStatusCode().value()) : HttpStatus.BAD_REQUEST;
        log.warn("Request rejected: {} - {}", status, ex.getMessage());
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), "Solicitud inválida: revisá los datos enviados"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        if (status.is5xxServerError()) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Request failed: {} - {}", status, reason != null ? reason : "", cause);
        } else {
            log.warn("Request rejected: {} - {}", status, reason != null ? reason : "");
        }

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
