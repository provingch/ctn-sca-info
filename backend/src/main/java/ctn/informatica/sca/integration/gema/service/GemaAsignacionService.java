package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.AsignacionDto;
import ctn.informatica.sca.integration.gema.dto.CursoRefDto;
import ctn.informatica.sca.integration.gema.dto.DocenteRefDto;
import ctn.informatica.sca.integration.gema.dto.MateriaRefDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 1: catálogo de asignaciones, sobre {@link AsignacionDao#findAll()} tal cual. Ese método
 * trae el curso_base (especialidad+nivel+sección, sin año): la promoción real se calcula con la
 * misma fórmula que {@link Curso#getCurso()} ({@code period - promocion + 3}, invertida) en vez de
 * agregar una consulta nueva — evita tocar {@code AsignacionDao}.
 */
@Service
public class GemaAsignacionService {

    private final AsignacionDao asignacionDao;
    private final ProfesorDao profesorDao;
    private final CursoDao cursoDao;

    public GemaAsignacionService(AsignacionDao asignacionDao, ProfesorDao profesorDao, CursoDao cursoDao) {
        this.asignacionDao = asignacionDao;
        this.profesorDao = profesorDao;
        this.cursoDao = cursoDao;
    }

    public List<AsignacionDto> listAsignaciones(Integer docenteCi, Integer cursoId) {
        Integer profesorId = docenteCi == null ? null : resolveProfesorId(docenteCi);
        if (docenteCi != null && profesorId == null) {
            return List.of();
        }
        Curso cursoFiltro = cursoId == null ? null : resolveCurso(cursoId);
        if (cursoId != null && cursoFiltro == null) {
            return List.of();
        }

        Map<Integer, Profesor> profesoresById = profesorDao.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Profesor::getId, p -> p, (a, b) -> a));

        try {
            return asignacionDao.findAll().stream()
                    .filter(a -> profesorId == null || a.getProfesorId() == profesorId)
                    .filter(a -> cursoFiltro == null || matchesCurso(a, cursoFiltro))
                    .map(a -> toDto(a, profesoresById.get(a.getProfesorId())))
                    .toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de asignaciones", ex);
        }
    }

    private Integer resolveProfesorId(int docenteCi) {
        return profesorDao.findAll().stream()
                .filter(p -> Objects.equals(p.getCi(), docenteCi))
                .map(Profesor::getId)
                .findFirst()
                .orElse(null);
    }

    private Curso resolveCurso(int cursoId) {
        try {
            return cursoDao.findById(cursoId);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de cursos", ex);
        }
    }

    private static boolean matchesCurso(Asignacion a, Curso curso) {
        int promocion = AcademicPeriod.current() - a.getCursoNivel() + 3;
        return Objects.equals(a.getEspecialidad(), curso.getEspecialidad())
                && promocion == curso.getPromocion()
                && Objects.equals(a.getCursoSeccion(), curso.getSeccion());
    }

    private static AsignacionDto toDto(Asignacion a, Profesor docente) {
        DocenteRefDto docenteRef = docente == null
                ? new DocenteRefDto(a.getProfesorId(), null, null, null)
                : new DocenteRefDto(docente.getId(), docente.getCi(), docente.getNombre(), docente.getApellido());
        MateriaRefDto materiaRef = new MateriaRefDto(a.getMateriaId(), a.getMateriaNombre());
        int promocion = AcademicPeriod.current() - a.getCursoNivel() + 3;
        CursoRefDto cursoRef = new CursoRefDto(a.getEspecialidad(), promocion, a.getCursoSeccion());
        return new AsignacionDto(a.getId(), docenteRef, materiaRef, cursoRef);
    }
}
