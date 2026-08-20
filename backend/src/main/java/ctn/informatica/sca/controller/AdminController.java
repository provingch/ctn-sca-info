package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Profesor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            MateriaDao materiaDao = new MateriaDao();
            ProfesorDao profesorDao = new ProfesorDao();
            PadreDao padreDao = new PadreDao();
            List<Materia> materiasDb = materiaDao.listAll();
            List<MateriaItem> materias = new java.util.ArrayList<>();
            for (Materia m : materiasDb) {
                materias.add(new MateriaItem(m.getId(), m.getNombre(), m.getCategoria(), materiaDao.listEspecialidadIdsForMateria(m.getId())));
            }
            List<UserItem> usuarios = new java.util.ArrayList<>();
            usuarios.addAll(profesorDao.findAll().stream().map(p -> new UserItem(p.getId(), p.getNombre(), p.getApellido(), p.getUsuario(), p.getNivel(), p.getCorreo(), p.getCi())).toList());
            usuarios.addAll(padreDao.findAll().stream().map(p -> new UserItem(p.getId(), p.getNombre(), p.getApellido(), p.getUsuario(), 4, p.getCorreo(), p.getCi())).toList());
            usuarios.sort((a, b) -> {
                int byApellido = String.valueOf(a.apellido()).compareToIgnoreCase(String.valueOf(b.apellido()));
                if (byApellido != 0) return byApellido;
                return Integer.compare(a.id(), b.id());
            });
            List<AssignmentItem> asignaciones = new AsignacionDao().findAll().stream().map(a -> new AssignmentItem(a.getId(), a.getProfesorId(), a.getMateriaId(), a.getCursoId(), a.getProfesorNombre(), a.getMateriaNombre(), a.getCursoDescripcion())).toList();
            List<StudentItem> alumnos = new AlumnoDao().findAll().stream().map(a -> new StudentItem(a.getId(), a.getNombre(), a.getApellido(), a.getCursoId(), a.getCi(), a.getCorreoEncargado(), a.getCorreoEncargado2())).toList();
            List<CourseItem> cursos = new CursoDao().findAll().stream().map(c -> new CourseItem(c.getId(), c.getEspecialidad(), c.getNivel(), c.getSeccion())).toList();
            List<SpecialtyItem> especialidades = new EspecialidadDao().findAll().stream().map(e -> new SpecialtyItem(e.getId(), e.getNombre())).toList();
            return new CatalogResponse(materias, usuarios, asignaciones, alumnos, cursos, especialidades);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar el panel administrativo", ex);
        }
    }

    @PutMapping("/materias/{id}")
    public void updateMateria(@PathVariable int id, @RequestBody MateriaInput input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null && notBlank(input.nombre()), "El nombre es requerido");
        validateMateriaEspecialidades(input);
        try {
            MateriaDao materiaDao = new MateriaDao();
            if (materiaDao.findById(id) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada");
            }
            String categoria = normalizeCategoria(input.categoria());
            if (!materiaDao.update(id, input.nombre().trim(), categoria)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada");
            }
            materiaDao.replaceEspecialidades(id, input.especialidadIds() == null ? java.util.List.of() : input.especialidadIds());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo actualizar la materia", ex);
        }
    }

    @PostMapping("/materias") @ResponseStatus(HttpStatus.CREATED)
    public void createMateria(@RequestBody MateriaInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()), "El nombre es requerido");
        validateMateriaEspecialidades(input);
        try {
            String categoria = normalizeCategoria(input.categoria());
            int id = new MateriaDao().create(input.nombre().trim(), categoria);
            if (input.especialidadIds() != null) new MateriaDao().replaceEspecialidades(id, input.especialidadIds());
        }
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
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && notBlank(input.usuario()) && input.ci() != null, "Nombre, apellido, usuario y cédula son requeridos");
        try {
            String defaultPassword = input.contrasenia() == null || input.contrasenia().isBlank() ? "password" : input.contrasenia();
            if (input.nivel() == 4) {
                PadreDao padreDao = new PadreDao();
                Padre padre = new Padre();
                padre.setNombre(input.nombre().trim());
                padre.setApellido(input.apellido().trim());
                padre.setUsuario(input.usuario().trim());
                padre.setContrasenia(defaultPassword);
                padre.setCorreo(input.correo());
                padre.setTelefono(input.telefono());
                padre.setCi(input.ci());
                padre.setTotpSecret(null);
                int id = new ProfesorDao().create(toProfesor(padre));
                if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario");
                if (!new ProfesorDao().updateNivel(id, 4)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo asignar el nivel del padre");
                }
                return;
            }
            Profesor p = new Profesor(); p.setNombre(input.nombre().trim()); p.setApellido(input.apellido().trim()); p.setUsuario(input.usuario().trim()); p.setContrasenia(defaultPassword); p.setNivel(input.nivel()); p.setCorreo(input.correo()); p.setCi(input.ci()); p.setTelefono(input.telefono() == null || input.telefono().isBlank() ? null : Integer.parseInt(input.telefono()));
            if (new ProfesorDao().create(p) <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo crear el usuario", ex);
        }
    }

    @PutMapping("/usuarios/{id}")
    public void updateUser(@PathVariable int id, @RequestBody UserInput input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && notBlank(input.usuario()) && input.ci() != null, "Nombre, apellido, usuario y cédula son requeridos");
        try {
            ProfesorDao profesorDao = new ProfesorDao();
            PadreDao padreDao = new PadreDao();
            Profesor targetProfesor = profesorDao.findById(id);
            Padre targetPadre = null;
            if (targetProfesor == null) {
                targetPadre = padreDao.findById(id);
            }
            if (targetProfesor == null && targetPadre == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
            }
            int nivel = targetProfesor != null ? targetProfesor.getNivel() : 4;
            if (nivel == 3) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los administradores no pueden editarse ni eliminarse desde este panel");
            }
            if (nivel == 4) {
                Padre existing = padreDao.findById(id);
                if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Padre no encontrado");
                Padre updated = new Padre();
                updated.setId(existing.getId());
                updated.setNombre(input.nombre().trim());
                updated.setApellido(input.apellido().trim());
                updated.setUsuario(input.usuario().trim());
                updated.setContrasenia(existing.getContrasenia() == null || existing.getContrasenia().isBlank() ? "password" : existing.getContrasenia());
                updated.setCorreo(existing.getCorreo());
                updated.setTelefono(input.telefono() == null || input.telefono().isBlank() ? existing.getTelefono() : input.telefono());
                updated.setCi(input.ci());
                updated.setTotpSecret(existing.getTotpSecret());
                if (!padreDao.update(updated)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Padre no encontrado");
                return;
            }
            Profesor existing = profesorDao.findById(id);
            if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
            Profesor updated = new Profesor();
            updated.setId(existing.getId());
            updated.setNombre(input.nombre().trim());
            updated.setApellido(input.apellido().trim());
            updated.setUsuario(input.usuario().trim());
            updated.setContrasenia(existing.getContrasenia() == null || existing.getContrasenia().isBlank() ? "password" : existing.getContrasenia());
            updated.setNivel(existing.getNivel());
            updated.setCorreo(existing.getCorreo());
            updated.setCi(input.ci());
            updated.setTelefono(input.telefono() == null || input.telefono().isBlank() ? existing.getTelefono() : Integer.parseInt(input.telefono()));
            updated.setCelular(existing.getCelular());
            updated.setFirmaImagen(existing.getFirmaImagen());
            updated.setFotoPerfil(existing.getFotoPerfil());
            updated.setTotpSecret(existing.getTotpSecret());
            if (!profesorDao.update(updated)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo actualizar el usuario", ex);
        }
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
            ProfesorDao profesorDao = new ProfesorDao();
            Profesor existing = profesorDao.findById(id);
            if (existing != null && existing.getNivel() == 3) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los administradores no pueden editarse ni eliminarse desde este panel");
            }
            if (!profesorDao.delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado o no se pudo eliminar");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar el usuario", ex); }
    }

    @GetMapping("/asignaciones/por-profesor/{profesorId}")
    public java.util.List<Asignacion> asignacionesPorProfesor(@PathVariable int profesorId, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            ProfesorDao profesorDao = new ProfesorDao();
            Profesor profesor = profesorDao.findById(profesorId);
            if (profesor == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesor no encontrado");
            }
            if (profesor.getNivel() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id indicado no corresponde a un profesor");
            }
            return new AsignacionDao().findByProfesor(profesorId);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo consultar las asignaciones del profesor", ex);
        }
    }

    @PostMapping("/asignaciones/batch") @ResponseStatus(HttpStatus.CREATED)
    public BatchAssignmentResponse createAssignmentsBatch(@RequestBody BatchAssignmentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null && input.profesorId() > 0 && input.materiaId() > 0 && input.cursoIds() != null && !input.cursoIds().isEmpty(), "profesorId, materiaId y cursoIds son requeridos");
        AsignacionDao dao = new AsignacionDao();
        int creadas = 0;
        int yaExistian = 0;
        try {
            for (Integer cursoId : input.cursoIds()) {
                if (cursoId == null || cursoId <= 0) continue;
                int result = dao.crear(input.profesorId(), input.materiaId(), cursoId);
                if (result > 0) creadas++;
                else if (result == -1) yaExistian++;
            }
            return new BatchAssignmentResponse(creadas, yaExistian);
        } catch (Exception ex) {
            throw failure("No se pudo crear la asignación en lote", ex);
        }
    }

    @PutMapping("/asignaciones/{id}")
    public void updateAssignment(@PathVariable int id, @RequestBody AssignmentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null && input.materiaId() > 0 && input.cursoId() > 0, "Materia y curso son requeridos");
        try {
            AsignacionDao dao = new AsignacionDao();
            Asignacion existente = dao.findById(id);
            if (existente == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
            if (dao.existe(existente.getProfesorId(), input.materiaId(), input.cursoId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignación ya existe");
            }
            if (!dao.actualizar(id, input.materiaId(), input.cursoId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo actualizar la asignación", ex);
        }
    }

    @DeleteMapping("/usuarios/{id}/google/clear")
    public GoogleClearResponse clearUsuarioGoogleTokens(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            boolean updated = new ProfesorDao().updateGoogleTokens(id, null, null, 0L, null);
            if (!updated) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado o no se pudo limpiar tokens");
            return new GoogleClearResponse("Tokens de Google eliminados para el usuario " + id);
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo limpiar tokens de Google", ex); }
    }

    @DeleteMapping("/alumnos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlumno(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new AlumnoDao().delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado o no se pudo eliminar");
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo eliminar el alumno", ex); }
    }

    @DeleteMapping("/ingresantes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngresante(@PathVariable int id, Authentication auth) {
        deleteAlumno(id, auth);
    }

    @PutMapping("/alumnos/{id}")
    public void updateAlumno(@PathVariable int id, @RequestBody StudentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && input.cursoId() > 0, "Nombre, apellido y curso son requeridos");
        try {
            AlumnoDao alumnoDao = new AlumnoDao();
            if (alumnoDao.findById(id) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
            }
            if (!alumnoDao.update(id, input.nombre().trim(), input.apellido().trim(), input.cursoId(), input.ci(), input.correoEncargado(), input.correoEncargado2())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo actualizar el alumno", ex);
        }
    }

    @PutMapping("/ingresantes/{id}")
    public void updateIngresante(@PathVariable int id, @RequestBody StudentInput input, Authentication auth) {
        updateAlumno(id, input, auth);
    }

    @PostMapping("/alumnos") @ResponseStatus(HttpStatus.CREATED)
    public void createStudent(@RequestBody StudentInput input, Authentication auth) {
        ApiAuth.requireUserId(auth); require(input != null && notBlank(input.nombre()) && notBlank(input.apellido()) && input.cursoId() > 0, "Nombre, apellido y curso son requeridos");
        try { new AlumnoDao().create(input.nombre().trim(), input.apellido().trim(), input.cursoId(), input.ci(), input.correoEncargado(), input.correoEncargado2()); } catch (Exception ex) { throw failure("No se pudo crear el alumno", ex); }
    }

    @PostMapping("/ingresantes") @ResponseStatus(HttpStatus.CREATED)
    public void createIngresante(@RequestBody StudentInput input, Authentication auth) {
        createStudent(input, auth);
    }

    @GetMapping("/padres/buscar")
    public java.util.List<Padre> buscarPadres(@RequestParam String q, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            return new PadreDao().buscar(q);
        } catch (Exception ex) {
            throw failure("No se pudo buscar padres", ex);
        }
    }

    @GetMapping("/alumnos/{id}/padres")
    public java.util.List<Padre> padresDelAlumno(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (new AlumnoDao().findById(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
            return new PadreDao().findPadresByAlumnoId(id);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo cargar los padres del alumno", ex);
        }
    }

    @PostMapping("/alumnos/{id}/padres/{padreId}") @ResponseStatus(HttpStatus.CREATED)
    public void linkPadre(@PathVariable int id, @PathVariable int padreId, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (new AlumnoDao().findById(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
            if (new PadreDao().findById(padreId) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Padre no encontrado");
            if (!new PadreDao().linkPadre(id, padreId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El vínculo alumno-padre ya existía");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo vincular el padre", ex);
        }
    }

    @DeleteMapping("/alumnos/{id}/padres/{padreId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkPadre(@PathVariable int id, @PathVariable int padreId, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new PadreDao().unlinkPadre(id, padreId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo no encontrado");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo desvincular el padre", ex);
        }
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
    private void validateMateriaEspecialidades(MateriaInput input) {
        if (input == null || input.especialidadIds() == null) {
            if ("comun".equalsIgnoreCase(normalizeCategoria(input == null ? null : input.categoria()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las materias comunes deben tener al menos 1 especialidad");
            }
            if ("especifico".equalsIgnoreCase(normalizeCategoria(input == null ? null : input.categoria()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las materias específicas deben tener exactamente 1 especialidad");
            }
            return;
        }
        String categoria = normalizeCategoria(input.categoria());
        if ("especifico".equalsIgnoreCase(categoria) && input.especialidadIds().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las materias específicas deben tener exactamente 1 especialidad");
        }
        if ("comun".equalsIgnoreCase(categoria) && input.especialidadIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las materias comunes deben tener al menos 1 especialidad");
        }
    }
    private String normalizeCategoria(String categoria) {
        String normalized = categoria == null ? "especifico" : categoria.trim().toLowerCase();
        if ("comun".equals(normalized) || "especifico".equals(normalized)) return normalized;
        return "especifico";
    }
    private Profesor toProfesor(Padre padre) {
        Profesor p = new Profesor();
        p.setId(padre.getId());
        p.setNombre(padre.getNombre());
        p.setApellido(padre.getApellido());
        p.setUsuario(padre.getUsuario());
        p.setContrasenia(padre.getContrasenia());
        p.setCi(padre.getCi());
        p.setCorreo(padre.getCorreo());
        p.setTelefono(padre.getTelefono() == null || padre.getTelefono().isBlank() ? null : Integer.valueOf(padre.getTelefono()));
        p.setNivel(4);
        return p;
    }
    private ResponseStatusException failure(String message, Exception ex) { return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, ex); }

    public record CatalogResponse(List<MateriaItem> materias, List<UserItem> usuarios, List<AssignmentItem> asignaciones, List<StudentItem> alumnos, List<CourseItem> cursos, List<SpecialtyItem> especialidades) {}
    public record MateriaItem(int id, String nombre, String categoria, List<Integer> especialidadIds) {}
    public record UserItem(int id, String nombre, String apellido, String usuario, int nivel, String correo, Integer ci) {}
    public record AssignmentItem(int id, int profesorId, int materiaId, int cursoId, String profesor, String materia, String curso) {}
    public record StudentItem(int id, String nombre, String apellido, int cursoId, Integer ci, String correoEncargado, String correoEncargado2) {}
    public record CourseItem(int id, String especialidad, int nivel, String seccion) {}
    public record SpecialtyItem(int id, String nombre) {}
    public record MateriaInput(String nombre, String categoria, List<Integer> especialidadIds) {}
    public record UserInput(String nombre, String apellido, String usuario, String contrasenia, int nivel, String correo, Integer ci, String telefono) {}
    public record AssignmentInput(int profesorId, int materiaId, int cursoId) {}
    public record BatchAssignmentInput(int profesorId, int materiaId, List<Integer> cursoIds) {}
    public record BatchAssignmentResponse(int creadas, int yaExistian) {}
    public record StudentInput(String nombre, String apellido, int cursoId, Integer ci, String correoEncargado, String correoEncargado2) {}
    public record WipeResponse(String message, int deletedGrades, int deletedTasks, int planillaId, int clearedGoogleCourseIds) {}
    public record GlobalWipeResponse(String message, int deletedGrades, int deletedTasks, int clearedGoogleCourseIds) {}
    public record GoogleClearResponse(String message) {}

    public record GoogleTokenInfo(String googleEmail, boolean hasAccessToken, boolean hasRefreshToken, long tokenExpiry) {}

    @GetMapping("/usuarios/{id}/google")
    public GoogleTokenInfo getUsuarioGoogleTokens(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            ProfesorDao dao = new ProfesorDao();
            ctn.informatica.sca.model.Profesor p = dao.findById(id);
            if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
            boolean hasAccess = p.getGcAccessToken() != null && !p.getGcAccessToken().isBlank();
            boolean hasRefresh = p.getGcRefreshToken() != null && !p.getGcRefreshToken().isBlank();
            long expiry = p.getGcTokenExpiry();
            return new GoogleTokenInfo(p.getGoogleEmail(), hasAccess, hasRefresh, expiry);
        } catch (ResponseStatusException ex) { throw ex; } catch (Exception ex) { throw failure("No se pudo obtener info de tokens de Google", ex); }
    }
}
