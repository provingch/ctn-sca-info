package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.HoraCatedraDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
import ctn.informatica.sca.dto.CreateHorarioSlotRequest;
import ctn.informatica.sca.dto.HoraCatedraDto;
import ctn.informatica.sca.dto.HorarioSlotDto;
import ctn.informatica.sca.dto.HorarioImportResponse;
import ctn.informatica.sca.dto.HorarioImportRowDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import ctn.informatica.sca.util.HorarioWorkbookBuilder;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/horario")
public class HorarioController {

    @GetMapping("/catalogo")
    public List<HoraCatedraDto> listCatalogo(Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            return new HoraCatedraDao().findAll().stream()
                    .map(this::toHoraCatedraDto)
                    .toList();
        } catch (Exception ex) {
            throw failure("No se pudo cargar el catálogo de horas cátedra", ex);
        }
    }

    @PostMapping("/catalogo")
    @ResponseStatus(HttpStatus.CREATED)
    public HoraCatedraDto createHoraCatedra(@RequestBody HoraCatedraDto input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null, "La hora cátedra es requerida");
        require(input.numero() > 0, "El número de la hora cátedra es requerido");
        require(input.horaInicio() != null && !input.horaInicio().isBlank(), "La hora de inicio es requerida");
        require(input.horaFin() != null && !input.horaFin().isBlank(), "La hora de fin es requerida");
        try {
            LocalTime horaInicio = LocalTime.parse(input.horaInicio());
            LocalTime horaFin = LocalTime.parse(input.horaFin());
            int id = new HoraCatedraDao().crear(input.numero(), input.etiqueta(), horaInicio, horaFin);
            return new HoraCatedraDto(id, input.numero(), input.etiqueta(), input.horaInicio(), input.horaFin());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo crear la hora cátedra", ex);
        }
    }

    @GetMapping("/resumen")
    public List<ctn.informatica.sca.dto.HorarioResumenCursoDto> resumen(Authentication auth) {
        ApiAuth.requireUserId(auth);
        Integer actingSpecialtyId = resolveCurrentSpecialtyAdminId(auth);
        if (actingSpecialtyId != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los administradores por especialidad no tienen acceso a este resumen global");
        }
        try {
            return new HorarioSlotDao().resumenPorCurso();
        } catch (Exception ex) {
            throw failure("No se pudo cargar el resumen de horarios por curso", ex);
        }
    }

    private Integer resolveCurrentSpecialtyAdminId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        int userId = ApiAuth.requireUserId(auth);
        var profesor = new ctn.informatica.sca.dao.ProfesorDao().findById(userId);
        if (profesor != null && profesor.getNivel() == 3) {
            return profesor.getEspecialidadId();
        }
        return null;
    }

    @GetMapping("/asignaciones/{asignacionId}")
    public List<HorarioSlotDto> listSlots(@PathVariable int asignacionId, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            Asignacion asignacion = new AsignacionDao().findById(asignacionId);
            if (asignacion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
            }
            return new HorarioSlotDao().findByAsignacion(asignacionId).stream()
                    .map(this::toHorarioSlotDto)
                    .toList();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo cargar el horario de la asignación", ex);
        }
    }

    @PostMapping("/asignaciones/{asignacionId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioSlotDto createSlot(@PathVariable int asignacionId, @RequestBody CreateHorarioSlotRequest input, Authentication auth) {
        ApiAuth.requireUserId(auth);
        require(input != null, "La solicitud es requerida");
        require(input.diaSemana() != null && input.diaSemana() >= 1 && input.diaSemana() <= 6, "diaSemana debe estar entre 1 y 6");
        require(input.horaCatedraId() != null && input.horaCatedraId() > 0, "horaCatedraId es requerido");

        try {
            AsignacionDao asignacionDao = new AsignacionDao();
            Asignacion asignacion = asignacionDao.findById(asignacionId);
            if (asignacion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
            }

            HorarioSlotDao dao = new HorarioSlotDao();
            if (dao.existeProfesorConflict(asignacion.getProfesorId(), input.diaSemana(), input.horaCatedraId())) {
                throw conflictProfesor(dao, asignacion, input.diaSemana(), input.horaCatedraId());
            }
            if (dao.existeCursoConflict(asignacion.getCursoId(), input.diaSemana(), input.horaCatedraId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese curso ya tiene otra materia asignada en ese día y hora.");
            }

            int id = dao.crear(asignacionId, asignacion.getProfesorId(), asignacion.getCursoId(), input.diaSemana(), input.horaCatedraId(), input.sala());
            HorarioSlot slot = new HorarioSlot();
            slot.setId(id);
            slot.setAsignacionId(asignacionId);
            slot.setUsuarioId(asignacion.getProfesorId());
            slot.setCursoId(asignacion.getCursoId());
            slot.setDiaSemana(input.diaSemana());
            slot.setHoraCatedraId(input.horaCatedraId());
            slot.setSala(input.sala());
            return toHorarioSlotDto(slot);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (SQLIntegrityConstraintViolationException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("uq_horario_profesor")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El profesor ya tiene una clase asignada en ese día y hora.");
            }
            if (message.contains("uq_horario_curso")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese curso ya tiene otra materia asignada en ese día y hora.");
            }
            throw failure("No se pudo crear el horario del profesor", ex);
        } catch (Exception ex) {
            throw failure("No se pudo crear el horario del profesor", ex);
        }
    }

    @PostMapping("/import/preview")
    public List<HorarioImportRowDto> importPreview(@RequestParam int cursoId, @RequestPart("file") MultipartFile file, Authentication auth) {
        authorizeCourse(cursoId, auth);
        try { return evaluateImport(new ctn.informatica.sca.util.HorarioWorkbookParser().parse(file.getInputStream(), cursoId), cursoId); }
        catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw failure("No se pudo leer el horario", ex); }
    }

    @PostMapping("/import/confirm")
    public HorarioImportResponse importConfirm(@RequestParam int cursoId, @RequestPart("file") MultipartFile file, Authentication auth) {
        authorizeCourse(cursoId, auth);
        try {
            HorarioSlotDao dao = new HorarioSlotDao();
            List<HorarioImportRowDto> rows = evaluateImport(new ctn.informatica.sca.util.HorarioWorkbookParser().parse(file.getInputStream(), cursoId), cursoId);
            List<HorarioImportRowDto> applied = rows.stream().filter(row -> "ok".equals(row.estado())).toList();
            dao.eliminarPorCurso(cursoId);
            for (HorarioImportRowDto row : applied) {
                Asignacion assignment = new AsignacionDao().findById(row.asignacionId());
                dao.crear(assignment.getId(), assignment.getProfesorId(), cursoId, row.diaSemana(), row.horaCatedraId(), null);
            }
            return new HorarioImportResponse(applied.size(), rows.size() - applied.size(), rows);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw failure("No se pudo confirmar la carga del horario", ex); }
    }

    private void authorizeCourse(int cursoId, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            Curso course = new CursoDao().findById(cursoId);
            if (course == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
            Integer specialty = resolveCurrentSpecialtyAdminId(auth);
            if (specialty != null && new CursoDao().findEspecialidadId(cursoId) != specialty) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permiso para gestionar este curso");
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw failure("No se pudo validar el curso", ex); }
    }

    private List<HorarioImportRowDto> evaluateImport(List<HorarioImportRowDto> parsed, int cursoId) throws Exception {
        HorarioSlotDao dao = new HorarioSlotDao();
        Set<String> occupied = new HashSet<>();
        Set<String> professors = new HashSet<>();
        List<HorarioImportRowDto> result = new ArrayList<>();
        for (HorarioImportRowDto row : parsed) {
            if (!"ok".equals(row.estado())) { result.add(row); continue; }
            Asignacion assignment = new AsignacionDao().findById(row.asignacionId());
            String key = row.diaSemana() + ":" + row.horaCatedraId();
            if (dao.existeProfesorConflict(assignment.getProfesorId(), row.diaSemana(), row.horaCatedraId())) {
                HorarioSlot occupying = dao.findProfesorConflictDetail(assignment.getProfesorId(), row.diaSemana(), row.horaCatedraId());
                if (occupying != null && occupying.getCursoId() == cursoId) occupying = null;
                if (occupying != null) { result.add(withStatus(row, "conflicto_profesor", conflictDetail(occupying))); continue; }
            }
            String professorKey = assignment.getProfesorId() + ":" + key;
            if (!occupied.add(key)) {
                result.add(withStatus(row, "conflicto_curso", "Ese curso ya tiene otra materia asignada en ese día y hora.")); continue;
            }
            if (!professors.add(professorKey)) {
                result.add(withStatus(row, "conflicto_profesor", "El profesor aparece en otra materia de esta misma carga en ese día y hora.")); continue;
            }
            result.add(row);
        }
        return result;
    }

    private HorarioImportRowDto withStatus(HorarioImportRowDto row, String status, String detail) {
        return new HorarioImportRowDto(row.diaSemana(), row.horaCatedraId(), row.horaCatedraEtiqueta(), row.materiaTexto(), row.profesorTexto(), row.asignacionId(), status, detail);
    }

    private ResponseStatusException conflictProfesor(HorarioSlotDao dao, Asignacion assignment, int day, int hour) {
        HorarioSlot occupying = null;
        try { occupying = dao.findProfesorConflictDetail(assignment.getProfesorId(), day, hour); }
        catch (Exception ignored) { }
        return new ResponseStatusException(HttpStatus.CONFLICT, "El profesor ya tiene una clase asignada en ese día y hora." + (occupying == null ? "" : " " + conflictDetail(occupying)));
    }

    private String conflictDetail(HorarioSlot slot) { return "Está dando " + slot.getMateriaNombre() + " en " + slot.getCursoDescripcion() + "."; }

    @DeleteMapping("/slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(@PathVariable int id, Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            if (!new HorarioSlotDao().eliminar(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot no encontrado");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("No se pudo eliminar el slot de horario", ex);
        }
    }

    @GetMapping("/export")
    public void export(@RequestParam int cursoId, Authentication auth, HttpServletResponse response) {
        ApiAuth.requireUserId(auth);
        try {
            Curso curso = new CursoDao().findById(cursoId);
            if (curso == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
            }
            List<HoraCatedra> horas = new HoraCatedraDao().findAll();
            List<HorarioSlot> slots = new HorarioSlotDao().findByCurso(cursoId);
            String base = "Horario_" + sanitize(curso.getEspecialidad()) + "_" + curso.getNivel() + curso.getSeccion();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(base + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
            try (XSSFWorkbook workbook = new HorarioWorkbookBuilder().build(curso, horas, slots)) {
                workbook.write(response.getOutputStream());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el horario del curso", ex);
        }
    }

    private HoraCatedraDto toHoraCatedraDto(HoraCatedra hora) {
        return new HoraCatedraDto(
                hora.getId(),
                hora.getNumero(),
                hora.getEtiqueta(),
                hora.getHoraInicio() == null ? null : hora.getHoraInicio().toString(),
                hora.getHoraFin() == null ? null : hora.getHoraFin().toString());
    }

    private HorarioSlotDto toHorarioSlotDto(HorarioSlot slot) {
        String horaInicio = slot.getHoraInicio();
        String horaFin = slot.getHoraFin();
        if (horaInicio == null || horaInicio.isBlank() || horaFin == null || horaFin.isBlank()) {
            try {
                HoraCatedra hora = new HoraCatedraDao().findAll().stream()
                        .filter(item -> item.getId() == slot.getHoraCatedraId())
                        .findFirst()
                        .orElse(null);
                if (hora != null) {
                    horaInicio = hora.getHoraInicio() == null ? null : hora.getHoraInicio().toString();
                    horaFin = hora.getHoraFin() == null ? null : hora.getHoraFin().toString();
                }
            } catch (Exception ignored) {
                // Si no se puede completar el catálogo, se conserva el valor ya presente en el slot.
            }
        }
        return new HorarioSlotDto(
                slot.getId(),
                slot.getAsignacionId(),
                slot.getDiaSemana(),
                slot.getHoraCatedraId(),
                slot.getHoraCatedraNumero() == null ? slot.getHoraCatedraId() : slot.getHoraCatedraNumero(),
                slot.getHoraCatedraEtiqueta(),
                horaInicio,
                horaFin,
                slot.getSala(),
                slot.getMateriaNombre(),
                slot.getCursoDescripcion(),
                slot.getProfesorNombre());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private ResponseStatusException failure(String message, Exception ex) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, ex);
    }

    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return "curso";
        }
        return text.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
