package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.util.ScaUiContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class EvaluacionCatalogController {

    @GetMapping("/instrumentos")
    public List<InstrumentoDto> listInstrumentos(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<Instrumento> instrumentos = new InstrumentoDao().findAll();
            List<InstrumentoDto> response = new ArrayList<>();
            for (Instrumento instrumento : instrumentos) {
                response.add(new InstrumentoDto(instrumento.getId(), instrumento.getNombre()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar instrumentos", ex);
        }
    }

    @GetMapping("/evaluacion/especialidades")
    public List<EspecialidadDto> listEspecialidades(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<Especialidad> especialidades = new EspecialidadDao().findAll();
            List<EspecialidadDto> response = new ArrayList<>();
            for (Especialidad especialidad : especialidades) {
                response.add(new EspecialidadDto(especialidad.getId(), especialidad.getNombre()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar especialidades", ex);
        }
    }

    @GetMapping("/evaluacion/cursos")
    public List<CursoEvaluacionDto> listCursos(
            @RequestParam(required = false) Integer especialidadId,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            List<Curso> cursos = new CursoDao().consultarCursos(userId);

            String selectedEspecialidad = null;
            if (especialidadId != null) {
                Especialidad especialidad = new EspecialidadDao().findById(especialidadId);
                if (especialidad == null) {
                    return List.of();
                }
                selectedEspecialidad = ScaUiContext.normalizeSpecialty(especialidad.getNombre());
            }

            List<CursoEvaluacionDto> response = new ArrayList<>();
            for (Curso curso : cursos) {
                if (selectedEspecialidad != null
                        && !selectedEspecialidad.equals(ScaUiContext.normalizeSpecialty(curso.getEspecialidad()))) {
                    continue;
                }
                response.add(new CursoEvaluacionDto(
                        curso.getId(),
                        curso.getEspecialidad(),
                        curso.getNivel(),
                        curso.getSeccion()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar cursos", ex);
        }
    }

    @GetMapping("/evaluacion/cursos/{cursoId}/clases")
    public List<ClaseRegistradaDto> listClases(@PathVariable int cursoId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            boolean canAccessCurso = new CursoDao().consultarCursos(userId)
                    .stream()
                    .anyMatch(curso -> curso.getId() == cursoId);
            if (!canAccessCurso) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso");
            }

            List<RasgoPlanilla> clases = new RasgoPlanillaDao().listarPorCurso(cursoId);
            List<ClaseRegistradaDto> response = new ArrayList<>();
            for (RasgoPlanilla clase : clases) {
                response.add(new ClaseRegistradaDto(
                        clase.getId(),
                        clase.getCursoId(),
                        clase.getProfesorId(),
                        clase.getTema(),
                        clase.getFechaClase(),
                        clase.getCreatedAt()));
            }
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar clases", ex);
        }
    }

    public record InstrumentoDto(int id, String nombre) {
    }

    public record EspecialidadDto(int id, String nombre) {
    }

    public record CursoEvaluacionDto(int id, String especialidad, int nivel, String seccion) {
    }

    public record ClaseRegistradaDto(
            int id,
            int cursoId,
            int profesorId,
            String tema,
            java.sql.Date fechaClase,
            java.sql.Timestamp createdAt) {
    }
}
