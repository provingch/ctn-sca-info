package ctn.informatica.sca.dto;

import java.util.List;

public record ProfileResponse(
        ProfileOwnerDto profileOwner,
        boolean isProfessorProfile,
        boolean isParentProfile,
        boolean isStaffProfile,
        String profileRoleLabel,
        String profileAccessDescription,
        boolean showMateriasPanel,
        boolean showGoogleClassroomPanel,
        boolean showSecurityPanel,
        boolean showActivityPanel,
        boolean canEditAdminOnlyProfileFields,
        boolean googleClassroomConnected,
        List<ProfileGoogleClassroomCourseDto> googleClassroomCourses,
        List<ProfileMateriaDto> teacherMaterias,
        List<AsignacionDto> misAsignaciones,
        List<ProfileMateriaDto> availableMaterias,
        List<EspecialidadDto> especialidades,
        String profesorEspecialidadNombre,
        String manualTeacherSubjectsText,
        List<String> activityLog,
        boolean totpEnabled,
        String pendingTotpSecret,
        String totpProvisioningUri,
        String pushPublicKey,
        boolean pushEnabled
) {
}
