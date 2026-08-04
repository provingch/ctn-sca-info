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

record CursoDto(
        int id,
        String especialidad,
        int curso,
        String seccion
) {
}

record PlanillaDto(
        int id,
        String nombre,
        String periodo,
        int tareasCount,
        int materiaId
) {
}

record HomeMateriaDto(
        int id,
        String nombre,
        String categoria
) {
}

record HomeGoogleClassroomCourseDto(
        String id,
        String name,
        String section,
        String room
) {
}

record RasgoPlanillaDto(
        int id,
        String tema,
        String fechaClase
) {
}

record RasgoAsistenciaDto(
        int id,
        String alumnoNombreCompleto,
        String estado,
        String faltaCodigo,
        String faltaObservacion
) {
}

record AlumnoDto(
        int id,
        String nombre,
        String apellido
) {
}

record InstrumentoDto(
        int id,
        String nombre
) {
}
