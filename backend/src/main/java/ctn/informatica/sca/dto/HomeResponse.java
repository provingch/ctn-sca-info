package ctn.informatica.sca.dto;

import java.util.List;
import java.util.Map;

public record HomeResponse(
        List<CursoDto> cursos,
        CursoDto selCurso,
        int selEtapa,
        String viewMode,
        List<PlanillaDto> planillas,
        boolean showPlanillaCards,
        Map<String, Integer> classroomPlanillaMap,
        Map<String, Integer> classroomPlanillaMateriaMap,
        List<HomeMateriaDto> materiasDetectadas,
        boolean googleClassroomConnected,
        String googleClassroomError,
        String googleClassroomPlaceholder,
        String googleClassroomVisibilityNotice,
        List<HomeGoogleClassroomCourseDto> googleClassroomCourses,
        List<RasgoPlanillaDto> rasgoPlanillas,
        RasgoPlanillaDto rasgoPlanillaSeleccionada,
        List<RasgoAsistenciaDto> rasgoAsistencias,
        List<AlumnoDto> rasgoAlumnosValidos,
        List<AlumnoDto> rasgoAlumnosInvalidos,
        List<InstrumentoDto> instrumentos
) {
}
