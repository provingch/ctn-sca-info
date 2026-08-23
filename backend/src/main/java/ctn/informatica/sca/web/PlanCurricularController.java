package ctn.informatica.sca.web;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.service.PlanCurricularParser;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.PlanCurricularTemplateBuilder;

@RestController
@RequestMapping("/api/plan-curricular")
public class PlanCurricularController {

    @Autowired
    private PlanCurricularParser parser;

    @Autowired
    private PlanCurricularDao dao;

    @Autowired
    private AsignacionDao asignacionDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PlanCurricularTemplateBuilder templateBuilder;

    private long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Long ? (Long) principal : Long.parseLong(principal.toString());
    }

    private User getCurrentUser() throws Exception {
        long userId = getCurrentUserId();
        return userDao.findById((int) userId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("asignacionId") int asignacionId, @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Archivo requerido");
        PlanCurricularDto dto;
        try {
            dto = parser.parse(file.getInputStream());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // validate header matches asignacion
        var asignacion = asignacionDao.findById(asignacionId);
        if (asignacion == null) return ResponseEntity.badRequest().body("Asignación inexistente");
        if (dto.disciplina != null && !dto.disciplina.equalsIgnoreCase(asignacion.getMateriaNombre()))
            return ResponseEntity.badRequest().body("Disciplina no coincide con la asignación");
        if (dto.especialidad != null && asignacion.getEspecialidad() != null && !dto.especialidad.equalsIgnoreCase(asignacion.getEspecialidad()))
            return ResponseEntity.badRequest().body("Especialidad no coincide con la asignación");
        if (dto.seccion != null && asignacion.getCursoSeccion() != null && !dto.seccion.equalsIgnoreCase(asignacion.getCursoSeccion()))
            return ResponseEntity.badRequest().body("Sección no coincide con la asignación");

        // save
        byte[] content = file.getBytes();
        int id = dao.saveOrReplace(asignacionId, dto.etapa, dto.anio, file.getOriginalFilename(), content, dto.temas);
        return ResponseEntity.ok().body("Saved plan id:" + id);
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(@RequestParam("asignacionId") int asignacionId) throws Exception {
        long userId = getCurrentUserId();
        var asignacion = asignacionDao.findById(asignacionId);
        if (asignacion == null) return ResponseEntity.notFound().build();
        if (asignacion.getProfesorId() != (int)userId) return ResponseEntity.status(403).build();
        byte[] data = templateBuilder.buildForAsignacion(asignacionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "plan-curricular-plantilla.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/mi-plan")
    public ResponseEntity<?> miPlan(@RequestParam("asignacionId") int asignacionId, @RequestParam("etapa") String etapa, @RequestParam("anio") int anio) throws Exception {
        long userId = getCurrentUserId();
        var asignacion = asignacionDao.findById(asignacionId);
        if (asignacion == null) return ResponseEntity.badRequest().body("Asignación inexistente");
        if (asignacion.getProfesorId() != (int)userId) return ResponseEntity.status(403).build();
        
        var plan = dao.findByAsignacion(asignacionId, etapa, anio);
        if (plan == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/asignaciones-disponibles")
    public ResponseEntity<?> asignacionesDisponibles(@RequestParam("cursoId") int cursoId) throws Exception {
        long userId = getCurrentUserId();
        var list = asignacionDao.findByProfesorAndCurso((int)userId, cursoId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> pendientes() throws Exception {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        // Only levels 2 (evaluador) and 3 (admin) can access this
        if (user.getLevel() < 2) return ResponseEntity.status(403).build();
        
        List<PlanCurricularDto> plans = dao.findPendientes();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanDetalle(@PathVariable int id) throws Exception {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        // Only levels 2 (evaluador) and 3 (admin) can access this
        if (user.getLevel() < 2) return ResponseEntity.status(403).build();
        
        var plan = dao.findById(id);
        if (plan == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable int id) throws Exception {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        // Only levels 2 (evaluador) and 3 (admin) can access this
        if (user.getLevel() < 2) return ResponseEntity.status(403).build();
        
        byte[] content = dao.getArchivoOriginal(id);
        if (content == null) return ResponseEntity.notFound().build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "plan-curricular.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable int id) throws Exception {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        // Only levels 2 (evaluador) and 3 (admin) can access this
        if (user.getLevel() < 2) return ResponseEntity.status(403).build();
        
        dao.aprobar(id, (int) getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable int id, @RequestParam("observaciones") String observaciones) throws Exception {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        // Only levels 2 (evaluador) and 3 (admin) can access this
        if (user.getLevel() < 2) return ResponseEntity.status(403).build();
        
        if (observaciones == null || observaciones.isBlank()) return ResponseEntity.badRequest().body("Observaciones requeridas");
        dao.rechazar(id, (int) getCurrentUserId(), observaciones);
        return ResponseEntity.ok().build();
    }
}
