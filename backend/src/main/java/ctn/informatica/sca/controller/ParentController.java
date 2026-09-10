package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import ctn.informatica.sca.util.ParentReportPdfBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/padre")
public class ParentController {
    private final PadreDao padreDao = new PadreDao();

    @GetMapping
    public ParentResponse getSummary(@RequestParam(required = false) Integer alumnoId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            List<Alumno> children = padreDao.findChildrenByPadreId(userId);
            int selected = resolveSelected(children, alumnoId);
            List<ChildDto> childDtos = new ArrayList<>();
            List<ParentSummaryItem> allSummary = padreDao.findParentSummary(userId);
            for (Alumno child : children) {
                int points = 0;
                int possible = 0;
                for (ParentSummaryItem item : allSummary) {
                    if (item.getAlumnoId() != null && item.getAlumnoId() == child.getId()) {
                        points += item.getPuntos();
                        possible += item.getTotalPosible();
                    }
                }
                childDtos.add(new ChildDto(child.getId(), child.getNombre(), child.getApellido(), child.getEspecialidadNombre(), possible == 0 ? 0 : (int) Math.round(points * 100.0 / possible)));
            }

            List<SubjectDto> subjects = new ArrayList<>();
            if (selected > 0) {
                for (ParentSummaryItem item : allSummary) {
                    if (item.getAlumnoId() == null || item.getAlumnoId() != selected) continue;
                    List<TaskDto> tasks = new ArrayList<>();
                    for (ParentTaskGrade task : padreDao.findTaskGradesForAlumnoPlanilla(selected, item.getPlanillaId())) {
                        tasks.add(new TaskDto(task.getTareaId(), task.getTareaTitulo(), task.getFecha(), task.getPuntos(), task.getTotal(), task.getEstado()));
                    }
                    subjects.add(new SubjectDto(item.getPlanillaId(), item.getMateriaId(), item.getMateriaNombre(), item.getEtapa(), item.getPuntos(), item.getTotalPosible(), item.getPorcentaje(), item.getNota(), tasks));
                }
            }
            boolean libretaDisponible = selected > 0 && padreDao.isLibretaDisponibleParaAlumno(selected);
            return new ParentResponse(childDtos, selected > 0 ? selected : null, subjects, libretaDisponible);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar el resumen académico", ex);
        }
    }

    private int resolveSelected(List<Alumno> children, Integer requested) {
        if (children.isEmpty()) return 0;
        if (requested != null && children.stream().anyMatch(child -> child.getId() == requested)) return requested;
        return children.get(0).getId();
    }

    @GetMapping(value = "/alumnos/{alumnoId}/reporte-mensual", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> reporteMensual(@PathVariable int alumnoId,
            @RequestParam int mes, @RequestParam int anio, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        if (mes < 1 || mes > 12 || anio < 2000 || anio > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mes o año inválidos.");
        }
        try {
            Alumno child = requireOwnChild(userId, alumnoId);
            List<ParentSummaryItem> items = summaryForAlumno(userId, alumnoId);
            List<ParentTaskGrade> tasks = padreDao.findTaskGradesForAlumno(alumnoId);
            try (PDDocument doc = new ParentReportPdfBuilder().buildMonthlyReport(
                    childName(child), child.getEspecialidadNombre(), mes, anio, items, tasks)) {
                return pdf(doc, "reporte-mensual-" + alumnoId + "-" + anio + "-" + String.format("%02d", mes) + ".pdf");
            }
        } catch (SQLException | IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el reporte mensual", ex);
        }
    }

    @GetMapping(value = "/alumnos/{alumnoId}/libreta", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> libreta(@PathVariable int alumnoId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Alumno child = requireOwnChild(userId, alumnoId);
            if (!padreDao.isLibretaDisponibleParaAlumno(alumnoId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La libreta estará disponible cuando el colegio cierre la Segunda Etapa.");
            }
            List<ParentSummaryItem> items = summaryForAlumno(userId, alumnoId);
            try (PDDocument doc = new ParentReportPdfBuilder().buildLibreta(
                    childName(child), child.getEspecialidadNombre(), items)) {
                return pdf(doc, "libreta-" + alumnoId + ".pdf");
            }
        } catch (SQLException | IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar la libreta", ex);
        }
    }

    /** Verifica que el alumno esté vinculado al padre autenticado; 403 si no. */
    private Alumno requireOwnChild(int userId, int alumnoId) throws SQLException {
        return padreDao.findChildrenByPadreId(userId).stream()
                .filter(child -> child.getId() == alumnoId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Ese alumno no está vinculado a tu cuenta."));
    }

    /** Ítems del resumen (mismos que la vista en pantalla) filtrados a un alumno. */
    private List<ParentSummaryItem> summaryForAlumno(int userId, int alumnoId) throws SQLException {
        List<ParentSummaryItem> out = new ArrayList<>();
        for (ParentSummaryItem item : padreDao.findParentSummary(userId)) {
            if (item.getAlumnoId() != null && item.getAlumnoId() == alumnoId) {
                out.add(item);
            }
        }
        return out;
    }

    private static String childName(Alumno child) {
        return (nz(child.getNombre()) + " " + nz(child.getApellido())).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private ResponseEntity<byte[]> pdf(PDDocument doc, String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(baos.toByteArray());
    }

    public record ParentResponse(List<ChildDto> hijos, Integer selectedAlumnoId, List<SubjectDto> materias, boolean libretaDisponible) {}
    public record ChildDto(int id, String nombre, String apellido, String especialidad, int promedio) {}
    public record SubjectDto(int planillaId, int materiaId, String materia, String etapa, int puntos, int total, int porcentaje, int nota, List<TaskDto> tareas) {}
    public record TaskDto(int id, String titulo, LocalDate fecha, Integer puntos, int total, String estado) {}
}
