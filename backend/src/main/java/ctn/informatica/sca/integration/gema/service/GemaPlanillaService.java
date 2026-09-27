package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.PlanillaDto;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 2: catálogo de planillas para que GEMA resuelva a cuál postear una tarea. Sobre
 * {@link PlanillaDao#findByCompositeKey} tal cual — ese método no recibe {@code periodo}, siempre
 * usa el período actual (mismo límite que el resto de la app); pedir otro período no rompe, pero
 * no encuentra nada.
 *
 * <p>Sin {@code fecha}, se devuelven las planillas de las dos etapas que existan (0, 1 o 2 filas).
 * Con {@code fecha}, se resuelve una sola: {@link Planilla#sugerirEtapaParaTarea} (la misma lógica
 * que ya usa {@code PlanillaProcesoWorkbookBuilder#filterTasksByEtapa} para decidir a qué etapa
 * pertenece una fecha) sobre la planilla que exista, para saber si corresponde la de etapa 1 o 2.
 */
@Service
public class GemaPlanillaService {

    private final PlanillaDao planillaDao;
    private final ProfesorDao profesorDao;

    public GemaPlanillaService(PlanillaDao planillaDao, ProfesorDao profesorDao) {
        this.planillaDao = planillaDao;
        this.profesorDao = profesorDao;
    }

    public List<PlanillaDto> listPlanillas(Integer docenteCi, Integer cursoId, Integer materiaId, Integer periodo, LocalDate fecha) {
        if (cursoId == null || materiaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursoId y materiaId son requeridos");
        }
        if (periodo != null && periodo != AcademicPeriod.current()) {
            return List.of();
        }
        Integer profesorId = null;
        if (docenteCi != null) {
            profesorId = resolveProfesorId(docenteCi);
            if (profesorId == null) {
                return List.of();
            }
        }

        try {
            List<Planilla> candidatas = resolveCandidatas(cursoId, materiaId, fecha);
            List<PlanillaDto> out = new ArrayList<>();
            for (Planilla p : candidatas) {
                if (profesorId != null && p.getProfesorId() != profesorId) {
                    continue;
                }
                out.add(toDto(p));
            }
            return out;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de planillas", ex);
        }
    }

    private List<Planilla> resolveCandidatas(int cursoId, int materiaId, LocalDate fecha) throws SQLException {
        Planilla etapa1 = planillaDao.findByCompositeKey(cursoId, materiaId, 1);
        Planilla etapa2 = planillaDao.findByCompositeKey(cursoId, materiaId, 2);
        if (fecha == null) {
            List<Planilla> out = new ArrayList<>();
            if (etapa1 != null) out.add(etapa1);
            if (etapa2 != null) out.add(etapa2);
            return out;
        }
        Planilla referencia = etapa1 != null ? etapa1 : etapa2;
        if (referencia == null) {
            return List.of();
        }
        int etapaIndex = referencia.sugerirEtapaParaTarea(fecha);
        Planilla target = etapaIndex == 1 ? etapa1 : etapa2;
        return target == null ? List.of() : List.of(target);
    }

    private Integer resolveProfesorId(int ci) {
        return profesorDao.findAll().stream()
                .filter(p -> Objects.equals(p.getCi(), ci))
                .map(Profesor::getId)
                .findFirst()
                .orElse(null);
    }

    private Integer resolveCi(int profesorId) {
        return profesorDao.findAll().stream()
                .filter(p -> p.getId() == profesorId)
                .map(Profesor::getCi)
                .findFirst()
                .orElse(null);
    }

    private PlanillaDto toDto(Planilla p) {
        return new PlanillaDto(p.getId(), p.getMateriaId(), p.getCursoId(), p.getPeriodo(), p.getEtapa(),
                resolveCi(p.getProfesorId()), p.isEtapa1Confirmada(), p.isEtapa2Confirmada());
    }
}
