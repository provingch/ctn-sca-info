package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Filtro por curso, etapa, período y materia que comparten la descarga de planillas y la vista de evaluación,
 * para que ambas resuelvan exactamente las mismas planillas.
 */
final class EvaluacionPlanillaSelector {

    record Match(Planilla planilla, String materiaNombre) {
    }

    record Selection(Curso curso, List<Match> matches) {
    }

    private EvaluacionPlanillaSelector() {
    }

    static int etapaIndex(String etapa) {
        return "segunda".equalsIgnoreCase(etapa) || "2".equals(etapa) ? 2 : 1;
    }

    static Selection select(PlanillaDao planillaDao, CursoDao cursoDao, EspecialidadDao especialidadDao,
            int cursoId, String etapa, int periodo, Integer materiaId) throws Exception {
        Curso curso = cursoDao.findById(cursoId);
        if (curso == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
        }
        int especialidadId = especialidadDao.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase(curso.getEspecialidad()))
                .map(e -> e.getId()).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada"));
        int etapaIndex = etapaIndex(etapa);
        List<Match> matches = new ArrayList<>();
        for (PlanillaDao.PlanillaInfo info : planillaDao.findPlanillasByCourse(especialidadId, curso.getPromocion(), curso.getSeccion(), periodo)) {
            Planilla planilla = planillaDao.findById(info.getPlanilla().getId());
            if (planilla == null || planilla.getEtapaIndex() != etapaIndex) continue;
            if (materiaId != null && materiaId.intValue() > 0 && planilla.getMateriaId() != materiaId.intValue()) continue;
            matches.add(new Match(planilla, info.getMateriaNombre()));
        }
        return new Selection(curso, matches);
    }
}
