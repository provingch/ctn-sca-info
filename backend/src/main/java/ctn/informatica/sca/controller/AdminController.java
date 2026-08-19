package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Profesor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TareaDao tareaDao;
    private final GradeDao gradeDao;
    private final PlanillaDao planillaDao;

    public AdminController() {
        this.tareaDao = new TareaDao();
        this.gradeDao = new GradeDao();
        this.planillaDao = new PlanillaDao();
    }

    AdminController(TareaDao tareaDao, GradeDao gradeDao, PlanillaDao planillaDao) {
        this.tareaDao = tareaDao;
        this.gradeDao = gradeDao;
        this.planillaDao = planillaDao;
    }
    @GetMapping
    public CatalogResponse catalog(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<MateriaItem> materias = new MateriaDao().listAll().stream().map(m -> new MateriaItem(m.getId(), m.getNombre(), m.getCategoria())).toList();
            List<UserItem> usuarios = new ProfesorDao().findAll().stream().map(p -> new UserItem(p.getId(), p.getNombre(), p.getApellido(), p.getUsuario(), p.getNivel(), p.getCorreo(), p.getEspecialidadId())).toList();
            List<AssignmentItem> asignaciones = new AsignacionDao().findAll().stream().map(a -> new AssignmentItem(a.getId(), a.getProfesorId(), a.getMateriaId(), a.getCursoId(), a.getProfesorNombre(), a.getMateriaNombre(), a.getCursoDescripcion())).toList();
            List<StudentItem> alumnos = new AlumnoDao().findAll().stream().map(a -> new StudentItem(a.getId(), a.getNombre(), a.getApellido(), a.getCursoId(), a.getCi(), a.getCorreoEncargado(), a.getCorreoEncargado2())).toList();
            List<CourseItem> cursos = new CursoDao().findAll().stream().map(c -> new CourseItem(c.getId(), c.getEspecialidad(), c.getNivel(), c.getSeccion())).toList();
            List<SpecialtyItem> especialidades = new EspecialidadDao().findAll().stream().map(e -> new SpecialtyItem(e.getId(), e.getNombre())).toList();
            return new CatalogResponse(materias, usuarios, asignaciones, alumnos, cursos, especialidades);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar el panel administrativo", ex);
        }

        // (no constructors injected here)
        }
    @PostMapping("/materias") @ResponseStatus(HttpStatus.CREATED)
    public void createMateria(@RequestBody MateriaInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()), "El nombre es requerido");
        try { int id = new MateriaDao().create(input.nombre().trim(), "comun".equalsIgnoreCase(input.categoria()) ? "comun" : "especifico"); if (input.especialidadIds() != null) new MateriaDao().replaceEspecialidades(id, input.especialidadIds()); }
        catch (Exception ex) { throw failure("No se pudo crear la materia", ex); }
    }

    @GetMapping("/materias/{id}/especialidades")
    public List<Integer> materiaEspecialidades(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            return new MateriaDao().listEspecialidadIdsForMateria(id);
        } catch (Exception ex) {
            throw failure("No se pudo cargar las especialidades de la materia", ex);
        }
    }

    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@RequestBody UserInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && notBlank(input.usuario()), "Nombre, apellido y usuario son requeridos");
        Profesor p = new Profesor(); p.setNombre(input.nombre().trim()); p.setApellido(input.apellido().trim()); p.setUsuario(input.usuario().trim()); p.setContrasenia(input.contrasenia()); p.setNivel(input.nivel()); p.setCorreo(input.correo()); p.setEspecialidadId(input.especialidadId());
        if (new ProfesorDao().create(p) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario");
    }

    @PostMapping("/asignaciones") @ResponseStatus(HttpStatus.CREATED)
    public void createAssignment(@RequestBody AssignmentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && input.profesorId() > 0 && input.materiaId() > 0 && input.cursoId() > 0, "Profesor, materia y curso son requeridos");
        try { if (new AsignacionDao().crear(input.profesorId(), input.materiaId(), input.cursoId()) <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignación ya existe"); }
        catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo crear la asignación", ex); }
    }

    @DeleteMapping("/asignaciones/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new AsignacionDao().eliminar(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar la asignación", ex); }
    }

    @DeleteMapping("/materias/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMateria(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            boolean deleted = new MateriaDao().delete(id);
            if (!deleted) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo eliminar: la materia está referenciada por planillas o no existe");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar la materia", ex); }
    }

    @DeleteMapping("/usuarios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUsuario(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new ProfesorDao().delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado o no se pudo eliminar");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar el usuario", ex); }
    }

    @PostMapping("/usuarios/{id}/google/clear")
    public GoogleClearResponse clearUsuarioGoogleTokens(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            boolean updated = new ProfesorDao().updateGoogleTokens(id, null, null, 0L, null);
            if (!updated) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado o no se pudo limpiar tokens");
            return new GoogleClearResponse("Tokens de Google eliminados para el usuario " + id);
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo limpiar tokens de Google", ex); }
    }

    @DeleteMapping("/ingresantes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngresante(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new AlumnoDao().delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingresante no encontrado o no se pudo eliminar");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar el ingresante", ex); }
    }

    @PostMapping("/ingresantes") @ResponseStatus(HttpStatus.CREATED)
    public void createStudent(@RequestBody StudentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && input.cursoId() > 0, "Nombre, apellido y curso son requeridos");
        try { new AlumnoDao().create(input.nombre().trim(), input.apellido().trim(), input.cursoId(), input.ci(), input.correoEncargado(), input.correoEncargado2()); } catch (Exception ex) { throw failure("No se pudo crear el ingresante", ex); }
    }

    @PostMapping("/planillas/{id}/sync/wipe")
    public WipeResponse wipePlanillaSync(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            int deletedGrades = this.gradeDao.deleteGradesForPlanilla(id);
            int deletedTasks = this.tareaDao.deleteImportedTasks(id);
            int clearedGoogleCourseIds = this.planillaDao.updateClassroomCourseId(id, null) ? 1 : 0;
            return new WipeResponse("Wipe de importaciones completado.", deletedGrades, deletedTasks, id, clearedGoogleCourseIds);
        } catch (Exception ex) {
            throw failure("No se pudo realizar wipe de sincronización", ex);
        }
    }

    @PostMapping("/sync/wipe-all")
    public GlobalWipeResponse wipeAllClassroomSync(Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            int deletedGrades = this.gradeDao.deleteImportedGradesForAllPlans();
            int deletedTasks = this.tareaDao.deleteImportedTasks(null);
            int clearedGoogleCourseIds = this.planillaDao.clearClassroomCourseIds();
            return new GlobalWipeResponse("Wipe global de sincronización Classroom completado.", deletedGrades, deletedTasks, clearedGoogleCourseIds);
        } catch (Exception ex) {
            throw failure("No se pudo realizar wipe global de sincronización", ex);
        }
    }

    private void require(boolean condition, String message) { if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private ResponseStatusException failure(String message, Exception ex) { return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, ex); }

    public record CatalogResponse(List<MateriaItem> materias, List<UserItem> usuarios, List<AssignmentItem> asignaciones, List<StudentItem> alumnos, List<CourseItem> cursos, List<SpecialtyItem> especialidades) {}
    public record MateriaItem(int id, String nombre, String categoria) {}
    public record UserItem(int id, String nombre, String apellido, String usuario, int nivel, String correo, Integer especialidadId) {}
    public record AssignmentItem(int id, int profesorId, int materiaId, int cursoId, String profesor, String materia, String curso) {}
    public record StudentItem(int id, String nombre, String apellido, int cursoId, Integer ci, String correoEncargado, String correoEncargado2) {}
    public record CourseItem(int id, String especialidad, int nivel, String seccion) {}
    public record SpecialtyItem(int id, String nombre) {}
    public record MateriaInput(String nombre, String categoria, List<Integer> especialidadIds) {}
    public record UserInput(String nombre, String apellido, String usuario, String contrasenia, int nivel, String correo, Integer especialidadId) {}
    public record AssignmentInput(int profesorId, int materiaId, int cursoId) {}
    public record StudentInput(String nombre, String apellido, int cursoId, Integer ci, String correoEncargado, String correoEncargado2) {}
    public record WipeResponse(String message, int deletedGrades, int deletedTasks, int planillaId, int clearedGoogleCourseIds) {}
    public record GlobalWipeResponse(String message, int deletedGrades, int deletedTasks, int clearedGoogleCourseIds) {}
    public record GoogleClearResponse(String message) {}
}
