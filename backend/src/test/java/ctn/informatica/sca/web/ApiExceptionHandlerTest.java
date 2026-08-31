package ctn.informatica.sca.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiExceptionHandlerTest {

    @Test
    void handleResponseStatus_logsCauseForServerErrors() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        IllegalStateException rootCause = new IllegalStateException("root cause");
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo generar el archivo",
                rootCause
        );

        Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ResponseEntity<ApiExceptionHandler.ApiError> response = handler.handleResponseStatus(ex);

            assertEquals(500, response.getStatusCode().value());
            assertEquals("No se pudo generar el archivo", response.getBody().message());

            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains("Request failed: 500 INTERNAL_SERVER_ERROR")
                            && event.getFormattedMessage().contains("No se pudo generar el archivo")
            ));
            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getThrowableProxy() != null && "root cause".equals(event.getThrowableProxy().getMessage())
            ));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
