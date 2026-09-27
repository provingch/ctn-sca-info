package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.integration.gema.dto.AlumnoDto;
import ctn.informatica.sca.integration.gema.dto.CursoRefDto;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 1: catálogo de alumnos, sobre {@link AlumnoDao#findAllActivos()}/{@code findAllEgresados()}
 * tal cual (sin tocarlos). {@code ci} y {@code cursoId} se filtran en memoria en vez de usar
 * {@code AlumnoDao#findByCursoId}, que no trae {@code ci} — así no se toca el DAO.
 */
@Service
public class GemaAlumnoService {

    private final AlumnoDao alumnoDao;
    private final CursoDao cursoDao;

    public GemaAlumnoService(AlumnoDao alumnoDao, CursoDao cursoDao) {
        this.alumnoDao = alumnoDao;
        this.cursoDao = cursoDao;
    }

    public List<AlumnoDto> listAlumnos(Integer cursoId, String ci, boolean egresados) {
        try {
            List<Alumno> alumnos = egresados ? alumnoDao.findAllEgresados() : alumnoDao.findAllActivos();
            Map<Integer, Curso> cursosById = egresados
                    ? Map.of()
                    : cursoDao.findAll().stream().collect(java.util.stream.Collectors.toMap(Curso::getId, Function.identity()));
            return alumnos.stream()
                    .filter(a -> cursoId == null || a.getCursoId() == cursoId)
                    .filter(a -> ci == null || ci.isBlank() || ci.trim().equals(a.getCi()))
                    .map(a -> toDto(a, cursosById.get(a.getCursoId())))
                    .toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de alumnos", ex);
        }
    }

    public AlumnoDto findByCi(String ci, boolean egresados) {
        return listAlumnos(null, ci, egresados).stream().findFirst().orElse(null);
    }

    /**
     * Para egresados, {@code AlumnoDao#findAllEgresados()} no trae la sección (no la
     * selecciona): la referencia de curso queda con {@code seccion = null} en ese caso.
     */
    private static AlumnoDto toDto(Alumno a, Curso cursoActivo) {
        CursoRefDto curso = cursoActivo != null
                ? new CursoRefDto(cursoActivo.getEspecialidad(), cursoActivo.getPromocion(), cursoActivo.getSeccion())
                : new CursoRefDto(a.getEspecialidadNombre(), a.getPromocion() == null ? 0 : a.getPromocion(), null);
        return new AlumnoDto(a.getId(), a.getCi(), a.getNombre(), a.getApellido(), a.getCursoId(), curso);
    }
}
