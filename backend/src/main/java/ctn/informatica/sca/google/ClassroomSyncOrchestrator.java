package ctn.informatica.sca.google;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.Tarea;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ClassroomSyncOrchestrator {

    public record ClassroomSyncResult(
            String googleCourseId,
            boolean classroomCourseMapped,
            int importedCourseworks,
            int linkedStudents,
            int importedGrades,
            String message) {
    }

    public ClassroomSyncResult syncPlanillaWithClassroom(Profesor profesor, Planilla planilla) throws IOException, SQLException {
        if (profesor == null || planilla == null) {
            return new ClassroomSyncResult(null, false, 0, 0, 0, "Datos de planilla o profesor no disponibles.");
        }

        if (!GoogleClassroomService.isGoogleConnected(profesor)) {
            return new ClassroomSyncResult(null, false, 0, 0, 0, "Google Classroom no está conectado para este profesor.");
        }

        Curso curso = new CursoDao().findById(planilla.getCursoId());
        if (curso == null) {
            return new ClassroomSyncResult(null, false, 0, 0, 0, "No se encontró el curso asociado a la planilla.");
        }

        Optional<Course> resolvedCourse = GoogleClassroomService.resolveCourseForPlanilla(profesor, curso, planilla, new PlanillaDao());
        if (resolvedCourse.isEmpty()) {
            return new ClassroomSyncResult(planilla.getGoogleCourseId(), false, 0, 0, 0,
                    "No se encontró un curso de Classroom compatible con esta planilla.");
        }

        Course classroomCourse = resolvedCourse.get();
        String classroomCourseId = classroomCourse.getId();
        boolean mapped = classroomCourseId != null && !classroomCourseId.isBlank();

        int importedCourseworks = importCourseworkForPlanilla(profesor, planilla, classroomCourse);
        List<Alumno> alumnos = new AlumnoDao().findByCursoId(planilla.getCursoId());
        int linkedStudents = GoogleClassroomService.syncStudentIdentities(profesor, classroomCourseId, alumnos);
        int importedGrades = importGradesForPlanilla(profesor, planilla, classroomCourse, alumnos);

        StringBuilder message = new StringBuilder();
        if (!mapped) {
            message.append("Curso Classroom no asociado.");
        } else {
            message.append("Curso Classroom asociado correctamente.");
        }
        if (importedCourseworks > 0) {
            message.append(" ").append(importedCourseworks).append(" tarea(s) importada(s)");
        }
        if (linkedStudents > 0) {
            if (message.length() > 0) {
                message.append(".");
            }
            message.append(" ").append(linkedStudents).append(" alumno(s) vinculados");
        }
        if (importedGrades > 0) {
            if (message.length() > 0) {
                message.append(".");
            }
            message.append(" ").append(importedGrades).append(" nota(s) importada(s)");
        }
        if (message.length() == 0) {
            message.append("Sincronización completada sin cambios.");
        } else {
            message.append(".");
        }

        return new ClassroomSyncResult(classroomCourseId, mapped, importedCourseworks, linkedStudents, importedGrades, message.toString());
    }

    private int importCourseworkForPlanilla(Profesor profesor, Planilla planilla, Course classroomCourse) {
        if (profesor == null || planilla == null || classroomCourse == null) {
            return 0;
        }

        try {
            TareaDao tareaDao = new TareaDao();
            Set<String> existingCourseworkIds = tareaDao.getGoogleCourseworkIdsForPlanilla(planilla.getId());
            List<CourseWork> courseWorks = GoogleClassroomService.listCourseWorkForCourse(profesor, classroomCourse.getId());
            if (courseWorks.isEmpty()) {
                return 0;
            }

            int imported = 0;
            int defaultInstrumentId = selectDefaultInstrumentId();
            for (CourseWork courseWork : courseWorks) {
                if (courseWork == null || courseWork.getId() == null) {
                    continue;
                }
                if (existingCourseworkIds.contains(courseWork.getId())) {
                    continue;
                }

                Tarea tarea = new Tarea();
                tarea.setPlanillaId(planilla.getId());
                tarea.setInstrumentoId(defaultInstrumentId);
                tarea.setTitulo(courseWork.getTitle() != null && !courseWork.getTitle().isBlank()
                        ? courseWork.getTitle()
                        : "Tarea Classroom");
                tarea.setFecha(resolveCourseWorkDate(courseWork));
                tarea.setFechaInicio(resolveCourseWorkStartDate(courseWork));
                tarea.setFechaLimite(resolveCourseWorkDueDate(courseWork));
                tarea.setTotal(resolveCourseWorkTotal(courseWork));
                tarea.setGoogleCourseworkId(courseWork.getId());
                tarea.setGoogleCourseworkUrl(resolveCourseWorkUrl(courseWork));

                tareaDao.insertarTarea(tarea);
                imported++;
            }
            return imported;
        } catch (IOException | SQLException ex) {
            System.err.println("Error importing Classroom coursework for planilla " + planilla.getId() + ": " + ex.getMessage());
            return 0;
        }
    }

    private int importGradesForPlanilla(Profesor profesor, Planilla planilla, Course classroomCourse, List<Alumno> alumnos) {
        if (profesor == null || planilla == null || classroomCourse == null || classroomCourse.getId() == null || classroomCourse.getId().isBlank()) {
            return 0;
        }

        try {
            Map<String, Integer> studentLookup = GoogleClassroomService.linkStudentsForCourse(profesor, classroomCourse.getId(), alumnos);
            if (studentLookup.isEmpty()) {
                return 0;
            }

            TareaDao tareaDao = new TareaDao();
            List<Tarea> tareas = tareaDao.consultarTarea(planilla.getId());
            Map<String, Integer> tareaIdByGoogleCourseworkId = new HashMap<>();
            for (Tarea tarea : tareas) {
                if (tarea.getGoogleCourseworkId() != null && !tarea.getGoogleCourseworkId().isBlank()) {
                    tareaIdByGoogleCourseworkId.put(tarea.getGoogleCourseworkId(), tarea.getId());
                }
            }
            if (tareaIdByGoogleCourseworkId.isEmpty()) {
                return 0;
            }

            RegistroDao registroDao = new RegistroDao();
            GradeDao gradeDao = new GradeDao();
            Map<Integer, Map<Integer, Integer>> gradesByRegistro = new HashMap<>();
            int imported = 0;

            for (Tarea tarea : tareas) {
                if (tarea.getGoogleCourseworkId() == null || tarea.getGoogleCourseworkId().isBlank()) {
                    continue;
                }
                List<com.google.api.services.classroom.model.StudentSubmission> submissions = GoogleClassroomService.listStudentSubmissionsForCourseWork(profesor, classroomCourse.getId(), tarea.getGoogleCourseworkId());
                for (com.google.api.services.classroom.model.StudentSubmission submission : submissions) {
                    if (submission == null || submission.getUserId() == null || submission.getUserId().isBlank()) {
                        continue;
                    }
                    Integer alumnoId = studentLookup.get(submission.getUserId());
                    if (alumnoId == null) {
                        continue;
                    }
                    Map<Integer, Integer> alumnoRegistroIds = registroDao.getRegistroIdsForPlanilla(planilla.getId(), Set.of(alumnoId));
                    Integer registroId = alumnoRegistroIds.get(alumnoId);
                    if (registroId == null) {
                        continue;
                    }
                    Double assignedGrade = submission.getAssignedGrade();
                    if (assignedGrade == null) {
                        assignedGrade = submission.getDraftGrade();
                    }
                    if (assignedGrade == null) {
                        continue;
                    }
                    int puntos = (int) Math.round(assignedGrade);
                    if (tarea.getTotal() > 0) {
                        puntos = Math.max(0, Math.min(tarea.getTotal(), puntos));
                    }
                    gradesByRegistro.computeIfAbsent(registroId, k -> new HashMap<>())
                            .put(tarea.getId(), puntos);
                    imported++;
                }
            }

            if (!gradesByRegistro.isEmpty()) {
                gradeDao.saveGradesBatch(planilla.getId(), gradesByRegistro);
            }
            return imported;
        } catch (IOException | SQLException ex) {
            System.err.println("Error importing Classroom grades for planilla " + planilla.getId() + ": " + ex.getMessage());
            return 0;
        }
    }

    private int selectDefaultInstrumentId() {
        try {
            InstrumentoDao instrumentoDao = new InstrumentoDao();
            var instrumentos = instrumentoDao.findAll();
            if (instrumentos == null || instrumentos.isEmpty()) {
                return 1;
            }
            for (var ins : instrumentos) {
                String nombre = ins.getNombre() != null ? ins.getNombre().toLowerCase() : "";
                if (nombre.contains("prueba")) {
                    return ins.getId();
                }
            }
            for (var ins : instrumentos) {
                String nombre = ins.getNombre() != null ? ins.getNombre().toLowerCase() : "";
                if (nombre.contains("trabajo") || nombre.contains("fichas")) {
                    return ins.getId();
                }
            }
            return instrumentos.get(0).getId();
        } catch (SQLException ex) {
            System.err.println("Unable to select default instrument id: " + ex.getMessage());
            return 1;
        }
    }

    private LocalDate resolveCourseWorkDate(CourseWork courseWork) {
        LocalDate due = resolveCourseWorkDueDate(courseWork);
        return due != null ? due : LocalDate.now();
    }

    private LocalDate resolveCourseWorkStartDate(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        String scheduledDate = courseWork.getScheduledTime();
        if (scheduledDate != null && !scheduledDate.isBlank()) {
            try {
                return java.time.OffsetDateTime.parse(scheduledDate).toLocalDate();
            } catch (Exception ex) {
                try {
                    return java.time.Instant.parse(scheduledDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private LocalDate resolveCourseWorkDueDate(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        var dueDate = courseWork.getDueDate();
        if (dueDate != null && dueDate.getYear() != null && dueDate.getMonth() != null && dueDate.getDay() != null) {
            try {
                return LocalDate.of(dueDate.getYear(), dueDate.getMonth(), dueDate.getDay());
            } catch (Exception ex) {
                // fallback to today
            }
        }
        return null;
    }

    private String resolveCourseWorkUrl(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        if (courseWork.getAlternateLink() != null && !courseWork.getAlternateLink().isBlank()) {
            return courseWork.getAlternateLink();
        }
        if (courseWork.getId() != null && !courseWork.getId().isBlank()) {
            return "https://classroom.google.com/c/" + courseWork.getId();
        }
        return null;
    }

    private int resolveCourseWorkTotal(CourseWork courseWork) {
        if (courseWork == null) {
            return 10;
        }
        Number maxPoints = courseWork.getMaxPoints();
        if (maxPoints != null) {
            int total = maxPoints.intValue();
            return total > 0 ? total : 10;
        }
        return 10;
    }
}
