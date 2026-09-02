package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PlanillaService {

    private final PlanillaDao planillaDao;
    private final TareaDao tareaDao;

    public PlanillaService() {
        this(new PlanillaDao(), new TareaDao());
    }

    public PlanillaService(PlanillaDao planillaDao, TareaDao tareaDao) {
        this.planillaDao = planillaDao;
        this.tareaDao = tareaDao;
    }

    public int reclasificarEtapas(int planillaId) throws SQLException {
        Planilla planilla = planillaDao.findById(planillaId);
        if (planilla == null) {
            return 0;
        }

        List<Tarea> tareas = tareaDao.consultarTarea(planillaId);
        if (tareas == null || tareas.isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (Tarea tarea : tareas) {
            if (tarea == null || tarea.getFecha() == null) {
                continue;
            }
            int etapaSugerida = planilla.sugerirEtapaParaTarea(tarea.getFecha());
            String nuevaEtapa = etapaSugerida == 2 ? "segunda" : "primera";
            if (nuevaEtapa.equalsIgnoreCase(planilla.getEtapa())) {
                continue;
            }
            planilla.setEtapa(nuevaEtapa);
            updated++;
        }
        return updated;
    }

    public int sugerirEtapaParaTarea(Planilla planilla, LocalDate fechaPublicacion) {
        if (planilla == null || fechaPublicacion == null) {
            return 1;
        }
        return planilla.sugerirEtapaParaTarea(fechaPublicacion);
    }
}
