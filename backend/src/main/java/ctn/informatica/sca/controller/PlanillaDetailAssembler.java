package ctn.informatica.sca.controller;

import ctn.informatica.sca.controller.PlanillaController.CursoDto;
import ctn.informatica.sca.controller.PlanillaController.GradeRangeDto;
import ctn.informatica.sca.controller.PlanillaController.GradeValueDto;
import ctn.informatica.sca.controller.PlanillaController.PlanillaDetailResponse;
import ctn.informatica.sca.controller.PlanillaController.PlanillaHeaderDto;
import ctn.informatica.sca.controller.PlanillaController.StudentRowDto;
import ctn.informatica.sca.controller.PlanillaController.TareaDto;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Armado del detalle de una planilla, compartido entre la vista del profesor y la de evaluación. */
final class PlanillaDetailAssembler {

    private PlanillaDetailAssembler() {
    }

    /**
     * Arma el detalle de una planilla (cabecera, tareas y notas por alumno). Lo usan el profesor (que edita) y
     * evaluación (sólo lectura). {@code ensureRegistroRows} crea las filas de registro que falten para los alumnos
     * del curso: el profesor lo necesita para poder cargar notas; una vista de sólo lectura no debe escribir.
     */
    static PlanillaDetailResponse build(Planilla planilla, boolean ensureRegistroRows) throws SQLException {
        Materia materia = null;
        if (planilla.getMateriaId() > 0) {
            materia = new MateriaDao().findById(planilla.getMateriaId());
            if (materia != null && materia.getNombre() != null && !materia.getNombre().isBlank()) {
                planilla.setNombre(materia.getNombre());
                planilla.setCategoria(materia.getCategoria());
            }
        }

        Curso curso = new CursoDao().findById(planilla.getCursoId());
        if (ensureRegistroRows) {
            new RegistroDao().ensureRegistroRowsForPlanilla(planilla.getId(), planilla.getCursoId());
        }

        List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla);
        Map<Integer, Integer> tareaMax = new LinkedHashMap<>();
        int totalPossiblePoints = 0;
        LocalDate maxEnd = null;

        List<TareaDto> tareasDto = new ArrayList<>();
        for (Tarea t : tareas) {
            tareasDto.add(new TareaDto(
                    t.getId(),
                    t.getPlanillaId(),
                    t.getInstrumentoId(),
                    t.getFecha(),
                    t.getTotal(),
                    t.getTitulo(),
                    t.getFechaInicio(),
                    t.getFechaLimite(),
                    t.getGoogleCourseworkId(),
                    t.getGoogleCourseworkUrl()));
            tareaMax.put(t.getId(), t.getTotal());
            totalPossiblePoints += t.getTotal();

            if (t.getFechaLimite() != null && (maxEnd == null || t.getFechaLimite().isAfter(maxEnd))) {
                maxEnd = t.getFechaLimite();
            }
        }

        // Con RSA activo el TP se amplía con sus puntos y la nota sale de ese TP ampliado.
        if (planilla.getRsaPuntos() != null) {
            totalPossiblePoints += planilla.getRsaPuntos();
        }
        planilla.computeGradeRanges(totalPossiblePoints);
        List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints);

        List<StudentRowDto> rowsDto = new ArrayList<>();
        for (StudentRow row : rows) {
            List<GradeValueDto> gradeValues = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : row.getGrades().entrySet()) {
                gradeValues.add(new GradeValueDto(entry.getKey(), entry.getValue()));
            }
            rowsDto.add(new StudentRowDto(
                    row.getRegistroId(),
                    row.getAlumnoId(),
                    row.getAlumnoNombre(),
                    gradeValues,
                    row.getTotal(),
                    row.getPorcentaje(),
                    row.getNota(),
                    row.getRsaPuntos()));
        }

        Map<String, GradeRangeDto> ranges = new LinkedHashMap<>();
        int li = planilla.getLimiteInferior();
        if (li > 0) {
            ranges.put("1", new GradeRangeDto(0, li - 1));
        }
        Map<Integer, int[]> computedRanges = planilla.getGradeRanges();
        if (computedRanges != null) {
            for (Map.Entry<Integer, int[]> entry : computedRanges.entrySet()) {
                int[] value = entry.getValue();
                if (value != null && value.length >= 2) {
                    ranges.put(String.valueOf(entry.getKey()), new GradeRangeDto(value[0], value[1]));
                }
            }
        }

        PlanillaHeaderDto header = new PlanillaHeaderDto(
                planilla.getId(),
                planilla.getCursoId(),
                planilla.getMateriaId(),
                planilla.getNombre(),
                planilla.getCategoria(),
                planilla.getEtapa(),
                planilla.getEtapaIndex(),
                planilla.getEtapaSugerida(),
                planilla.getPeriodo(),
                planilla.getProfesorId(),
                (int) Math.round(100 * planilla.getExigencia()),
                totalPossiblePoints,
                AcademicPeriod.etapaStartDate(
                        planilla.getPeriodo() > 0 ? planilla.getPeriodo() : AcademicPeriod.current(),
                        planilla.getEtapaIndex()),
                maxEnd,
                planilla.getFechaCierreEtapa1(),
                planilla.getEtapa1Confirmada(),
                planilla.getFechaCierreEtapa2(),
                planilla.getEtapa2Confirmada(),
                planilla.getGoogleCourseId(),
                planilla.getRsaPuntos(),
                planilla.getRsaToleranciaValor(),
                planilla.getRsaToleranciaUnidad());

        CursoDto cursoDto = curso == null
                ? null
                : new CursoDto(curso.getId(), curso.getEspecialidad(), curso.getSeccion(), curso.getNivel());

        return new PlanillaDetailResponse(
                header,
                cursoDto,
                tareasDto,
                rowsDto,
                ranges,
                Collections.emptyList());
    }

    static List<Tarea> filterTasksByEtapa(List<Tarea> tareas, Planilla planilla) {
        if (tareas == null || tareas.isEmpty()) {
            return tareas;
        }

        int planillaEtapaIndex = planilla.getEtapaIndex();
        List<Tarea> filtered = new ArrayList<>();
        for (Tarea tarea : tareas) {
            if (planilla.sugerirEtapaParaTarea(tarea.getFecha()) == planillaEtapaIndex) {
                filtered.add(tarea);
            }
        }
        return filtered;
    }
}
