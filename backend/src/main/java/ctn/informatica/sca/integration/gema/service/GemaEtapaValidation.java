package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.model.Planilla;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de etapa compartidas por tareas y calificaciones de GEMA: la misma que ya usa
 * {@code PlanillaProcesoWorkbookBuilder#filterTasksByEtapa} ({@link Planilla#sugerirEtapaParaTarea}).
 */
final class GemaEtapaValidation {

    private GemaEtapaValidation() {
    }

    static void requireEtapaAbierta(Planilla planilla) {
        boolean confirmada = planilla.getEtapaIndex() == 2 ? planilla.isEtapa2Confirmada() : planilla.isEtapa1Confirmada();
        if (confirmada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La etapa de esta planilla ya está confirmada");
        }
    }

    static void requireFechaEnEtapa(Planilla planilla, LocalDate fecha) {
        if (fecha != null && planilla.sugerirEtapaParaTarea(fecha) != planilla.getEtapaIndex()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La fecha límite de la tarea no pertenece a una etapa activa de la planilla");
        }
    }
}
