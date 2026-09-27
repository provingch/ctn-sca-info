package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.integration.gema.dto.CursoDto;
import ctn.informatica.sca.model.Curso;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Fase 1: catálogo de cursos, sobre {@link CursoDao#findAll()} tal cual (sin tocarlo). */
@Service
public class GemaCursoService {

    private final CursoDao cursoDao;

    public GemaCursoService(CursoDao cursoDao) {
        this.cursoDao = cursoDao;
    }

    public List<CursoDto> listCursos(String especialidad) {
        try {
            List<Curso> cursos = cursoDao.findAll();
            return cursos.stream()
                    .filter(c -> especialidad == null || especialidad.isBlank()
                            || especialidad.trim().equalsIgnoreCase(c.getEspecialidad()))
                    .map(GemaCursoService::toDto)
                    .toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de cursos", ex);
        }
    }

    static CursoDto toDto(Curso curso) {
        return new CursoDto(curso.getId(), curso.getEspecialidad(), curso.getPromocion(), curso.getSeccion());
    }
}
