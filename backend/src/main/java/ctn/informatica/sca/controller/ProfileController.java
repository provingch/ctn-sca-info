package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.PushSubscriptionDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.google.GoogleClassroomService;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.PasswordUtil;
import ctn.informatica.sca.util.RememberMeTokenStore;
import ctn.informatica.sca.util.ScaUiContext;
import ctn.informatica.sca.util.TotpUtils;
import ctn.informatica.sca.util.PushNotificationService;
import com.google.api.services.classroom.model.Course;
import ctn.informatica.sca.dto.ChangePasswordRequest;
import ctn.informatica.sca.dto.ConfirmTotpRequest;
import ctn.informatica.sca.dto.ProfileGoogleClassroomCourseDto;
import ctn.informatica.sca.dto.ProfileMateriaDto;
import ctn.informatica.sca.dto.ProfileOwnerDto;
import ctn.informatica.sca.dto.ProfileResponse;
import ctn.informatica.sca.dto.AsignacionDto;
import ctn.informatica.sca.dto.EspecialidadDto;
import ctn.informatica.sca.dto.SaveProfileRequest;
import ctn.informatica.sca.dto.SelectUiSpecialtyRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Map<Integer, String> pendingTotpSecrets = new ConcurrentHashMap<>();

    private final AsignacionDao asignacionDao;
    private final CursoDao cursoDao;
    private final EspecialidadDao especialidadDao;
    private final MateriaDao materiaDao;
    private final PadreDao padreDao;
    private final ProfesorDao profesorDao;
    private final PushSubscriptionDao pushSubscriptionDao;
    private final UserDao userDao;

    public ProfileController() {
        this(new AsignacionDao(), new CursoDao(), new EspecialidadDao(), new MateriaDao(), new PadreDao(), new ProfesorDao(), new PushSubscriptionDao(), new UserDao());
    }

    @Autowired
    public ProfileController(
            AsignacionDao asignacionDao,
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            MateriaDao materiaDao,
            PadreDao padreDao,
            ProfesorDao profesorDao,
            PushSubscriptionDao pushSubscriptionDao,
            UserDao userDao) {
        this.asignacionDao = asignacionDao;
        this.cursoDao = cursoDao;
        this.especialidadDao = especialidadDao;
        this.materiaDao = materiaDao;
        this.padreDao = padreDao;
        this.profesorDao = profesorDao;
        this.pushSubscriptionDao = pushSubscriptionDao;
        this.userDao = userDao;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public ProfileResponse getProfile(Authentication authentication) {
        User user = requireUser(authentication);
        Profesor profesor = null;
        Padre padre = null;
        boolean isProfessorProfile = user.getLevel() == 1;
        boolean isStaffProfile = user.getLevel() >= 1 && user.getLevel() <= 3;
        boolean isParentProfile = user.getLevel() == 4;

        if (isStaffProfile) {
            profesor = profesorDao.findById(user.getId());
        } else if (isParentProfile) {
            try {
                padre = padreDao.findById(user.getId());
            } catch (SQLException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar el perfil familiar", ex);
            }
        }

        boolean googleClassroomConnected = false;
        List<Course> googleClassroomCourses = Collections.emptyList();
        List<Materia> teacherMaterias = Collections.emptyList();
        List<Materia> availableMaterias = Collections.emptyList();
        List<Asignacion> misAsignaciones = Collections.emptyList();
        List<Especialidad> especialidades = Collections.emptyList();
        String manualTeacherSubjectsText = "";

        if (profesor != null) {
            googleClassroomConnected = GoogleClassroomService.isGoogleConnected(profesor);
            try {
                teacherMaterias = materiaDao.listByProfesor(profesor.getId());
                availableMaterias = materiaDao.listAvailableForProfesor(profesor.getId());
                misAsignaciones = asignacionDao.findByProfesor(profesor.getId());
                manualTeacherSubjectsText = profesorDao.findManualSubjectsText(profesor.getId());
            } catch (SQLException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar datos del perfil", ex);
            }
        }

        if (profesor != null && googleClassroomConnected) {
            try {
                List<ctn.informatica.sca.model.Curso> cursos = cursoDao.consultarCursos(user.getId());
                List<String> subjectNames = materiaNames(teacherMaterias);
                subjectNames.addAll(parseManualSubjects(manualTeacherSubjectsText));
                googleClassroomCourses = GoogleClassroomService.listAllowedCourses(profesor, cursos, subjectNames);
            } catch (Exception ex) {
                googleClassroomCourses = Collections.emptyList();
            }
        }

        try {
            especialidades = especialidadDao.findAll();
        } catch (Exception ex) {
            especialidades = Collections.emptyList();
        }

        String totpSecret = null;
        if (profesor != null) {
            totpSecret = profesor.getTotpSecret();
        } else if (padre != null) {
            totpSecret = padre.getTotpSecret();
        }
        String pendingTotpSecret = pendingTotpSecrets.get(user.getId());

        return new ProfileResponse(
                toProfileOwnerDto(profesor, padre),
                isProfessorProfile,
                isParentProfile,
                isStaffProfile,
                roleLabel(user),
                accessDescription(user),
                isProfessorProfile,
                isProfessorProfile,
                true,
                true,
                canModifyField("nombre", user),
                googleClassroomConnected,
                googleClassroomCourses.stream().map(this::toProfileGoogleClassroomCourseDto).collect(Collectors.toList()),
                teacherMaterias.stream().map(this::toProfileMateriaDto).collect(Collectors.toList()),
                misAsignaciones.stream().map(this::toAsignacionDto).collect(Collectors.toList()),
                availableMaterias.stream().map(this::toProfileMateriaDto).collect(Collectors.toList()),
                especialidades.stream().map(this::toEspecialidadDto).collect(Collectors.toList()),
                resolveProfesorEspecialidadNombre(profesor),
                manualTeacherSubjectsText,
                Collections.emptyList(),
                totpSecret != null && !totpSecret.isBlank(),
                pendingTotpSecret,
                pendingTotpSecret == null || pendingTotpSecret.isBlank() ? null
                        : TotpUtils.getOtpAuthUrl("SCA", user.getUsername(), pendingTotpSecret),
                PushNotificationService.resolveVapidPublicKey(),
                isPushEnabled(user)
        );
    }

    @PostMapping("/select-ui-specialty")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectUiSpecialty(
            @RequestBody SelectUiSpecialtyRequest request,
            Authentication authentication) {
        requireUser(authentication);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El request es requerido.");
        }

        if ("general".equals(ScaUiContext.normalizeSpecialty(request.specialtyToken()))) {
            return;
        }

        Especialidad selected = resolveUiEspecialidad(request.specialtyId(), request.specialtyName(), request.specialtyToken());
        if (selected == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La especialidad seleccionada no es válida.");
        }
    }

    @PostMapping("/prepare-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void prepareTotp(Authentication authentication) {
        User user = requireUser(authentication);
        String generatedSecret = TotpUtils.generateSecret();
        pendingTotpSecrets.put(user.getId(), generatedSecret);
    }

    @PostMapping("/confirm-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmTotp(
            @RequestBody ConfirmTotpRequest request,
            Authentication authentication) {
        User user = requireUser(authentication);
        if (request == null || request.totpSetupCode() == null || request.totpSetupCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de la app es requerido.");
        }

        String pendingSecret = pendingTotpSecrets.get(user.getId());
        if (pendingSecret == null || pendingSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay un código de configuración activo. Genera un nuevo código.");
        }

        boolean verified = TotpUtils.verifyCode(pendingSecret, request.totpSetupCode().trim());
        if (!verified) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de autenticación no es válido. Intenta de nuevo.");
        }

        try {
            if (!saveTotpSecretForUser(user, pendingSecret)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo activar 2FA. Intenta de nuevo más tarde.");
            }
            pendingTotpSecrets.remove(user.getId());
        } catch (SQLException ex) {
            pendingTotpSecrets.remove(user.getId());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el secreto de 2FA.", ex);
        }
    }

    @PostMapping("/disable-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableTotp(Authentication authentication) {
        User user = requireUser(authentication);
        try {
            if (!saveTotpSecretForUser(user, null)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo desactivar 2FA. Intenta de nuevo más tarde.");
            }
            pendingTotpSecrets.remove(user.getId());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al desactivar 2FA.", ex);
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El request es requerido.");
        }

        boolean hasCurrentPassword = request.currentPassword() != null && !request.currentPassword().trim().isEmpty();
        boolean hasNewPassword = request.newPassword() != null && !request.newPassword().trim().isEmpty();
        boolean hasConfirmPassword = request.confirmPassword() != null && !request.confirmPassword().trim().isEmpty();

        if (!hasCurrentPassword && !hasNewPassword && !hasConfirmPassword) {
            return;
        }

        List<String> errors = new ArrayList<>();
        if (!hasCurrentPassword) {
            errors.add("La contraseña actual es requerida.");
        }
        if (!hasNewPassword) {
            errors.add("La nueva contraseña es requerida.");
        } else if (request.newPassword().length() < 6) {
            errors.add("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (!hasConfirmPassword) {
            errors.add("La confirmación de contraseña es requerida.");
        } else if (!request.newPassword().equals(request.confirmPassword())) {
            errors.add("Las contraseñas no coinciden.");
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
        }

        User user = requireUser(authentication);
        try {
            boolean passwordUpdated = false;
            if (user.getLevel() >= 1 && user.getLevel() <= 3) {
                Profesor profesor = profesorDao.findById(user.getId());
                if (profesor != null && profesor.getContrasenia() != null && PasswordUtil.matches(request.currentPassword(), profesor.getContrasenia())) {
                    profesor.setContrasenia(request.newPassword());
                    passwordUpdated = profesorDao.update(profesor);
                } else {
                    errors.add("La contraseña actual es incorrecta.");
                }
            } else if (user.getLevel() == 4) {
                Padre padre = padreDao.findById(user.getId());
                if (padre != null && padre.getContrasenia() != null && PasswordUtil.matches(request.currentPassword(), padre.getContrasenia())) {
                    padre.setContrasenia(request.newPassword());
                    passwordUpdated = padreDao.update(padre);
                } else {
                    errors.add("La contraseña actual es incorrecta.");
                }
            } else {
                errors.add("Este tipo de usuario no admite cambio de contraseña desde este perfil.");
            }

            if (!errors.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
            }
            if (!passwordUpdated) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar la contraseña.");
            }
            RememberMeTokenStore.invalidateUserTokens(user.getId());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar la contraseña.", ex);
        }
    }

    @PostMapping("/save-profile")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveProfile(
            @RequestBody SaveProfileRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El request es requerido.");
        }

        User user = requireUser(authentication);
        List<String> errors = new ArrayList<>();

        if (request.usuario() == null || request.usuario().trim().isEmpty()) {
            errors.add("El nombre de usuario no puede estar vacío.");
        }

        try {
            if (user.getLevel() == 4) {
                Padre padre = padreDao.findById(user.getId());
                if (padre == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se pudo cargar el perfil del usuario.");
                }
                padre.setUsuario(request.usuario().trim());
                padre.setCorreo(request.correo() == null ? null : request.correo().trim());
                padre.setTelefono(request.telefono() == null ? null : request.telefono().trim());
                if (request.nombre() != null && !request.nombre().trim().isEmpty()) {
                    if (canModifyField("nombre", user)) {
                        padre.setNombre(request.nombre().trim());
                    } else {
                        errors.add("Solo el administrador puede modificar el nombre.");
                    }
                }
                if (request.apellido() != null && !request.apellido().trim().isEmpty()) {
                    if (canModifyField("apellido", user)) {
                        padre.setApellido(request.apellido().trim());
                    } else {
                        errors.add("Solo el administrador puede modificar el apellido.");
                    }
                }
                if (request.ci() != null) {
                    if (canModifyField("ci", user)) {
                        padre.setCi(request.ci());
                    } else {
                        errors.add("Solo el administrador puede modificar la cédula.");
                    }
                }
                if (!errors.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
                }
                if (!padreDao.update(padre)) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron guardar los datos. Intente de nuevo más tarde.");
                }
                return;
            }

            Profesor profesor = profesorDao.findById(user.getId());
            if (profesor == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se pudo cargar el perfil del usuario.");
            }

            if (request.telefono() != null && !request.telefono().trim().isEmpty()) {
                try {
                    profesor.setTelefono(Integer.valueOf(request.telefono().trim()));
                } catch (NumberFormatException ex) {
                    errors.add("Teléfono inválido: debe contener sólo dígitos.");
                }
            }
            if (request.celular() != null && !request.celular().trim().isEmpty()) {
                try {
                    profesor.setCelular(Integer.valueOf(request.celular().trim()));
                } catch (NumberFormatException ex) {
                    errors.add("Celular inválido: debe contener sólo dígitos.");
                }
            }
            profesor.setUsuario(request.usuario().trim());
            if (request.correo() != null) {
                profesor.setCorreo(request.correo().trim());
            }
            if (request.nombre() != null && !request.nombre().trim().isEmpty()) {
                if (canModifyField("nombre", user)) {
                    profesor.setNombre(request.nombre().trim());
                } else {
                    errors.add("Solo el administrador puede modificar el nombre.");
                }
            }
            if (request.apellido() != null && !request.apellido().trim().isEmpty()) {
                if (canModifyField("apellido", user)) {
                    profesor.setApellido(request.apellido().trim());
                } else {
                    errors.add("Solo el administrador puede modificar el apellido.");
                }
            }
            if (request.ci() != null) {
                if (canModifyField("ci", user)) {
                    profesor.setCi(request.ci());
                } else {
                    errors.add("Solo el administrador puede modificar la cédula.");
                }
            }
            if (request.nivel() != null) {
                if (canModifyField("nivel", user)) {
                    profesor.setNivel(request.nivel());
                } else {
                    errors.add("Solo el administrador puede modificar el nivel.");
                }
            }
            if (request.firmaImagen() != null) {
                profesor.setFirmaImagen(request.firmaImagen().trim().isEmpty() ? null : request.firmaImagen().trim());
            }

            if (!errors.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
            }
            if (!profesorDao.update(profesor)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron guardar los datos. Intente de nuevo más tarde.");
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el perfil.", ex);
        }
    }

    private ProfileOwnerDto toProfileOwnerDto(Profesor profesor, Padre padre) {
        if (profesor != null) {
            return new ProfileOwnerDto(
                    profesor.getId(),
                    profesor.getNombre(),
                    profesor.getApellido(),
                    (profesor.getNombre() == null ? "" : profesor.getNombre()) + " " + (profesor.getApellido() == null ? "" : profesor.getApellido()),
                    profesor.getCi(),
                    profesor.getCorreo(),
                    profesor.getTelefono() == null ? null : String.valueOf(profesor.getTelefono()),
                    profesor.getCelular() == null ? null : String.valueOf(profesor.getCelular()),
                    profesor.getUsuario(),
                    profesor.getGoogleEmail(),
                    profesor.getGcAccessToken(),
                    profesor.getFirmaImagen()
            );
        }
        if (padre != null) {
            return new ProfileOwnerDto(
                    padre.getId(),
                    padre.getNombre(),
                    padre.getApellido(),
                    (padre.getNombre() == null ? "" : padre.getNombre()) + " " + (padre.getApellido() == null ? "" : padre.getApellido()),
                    padre.getCi(),
                    padre.getCorreo(),
                    padre.getTelefono(),
                    null,
                    padre.getUsuario(),
                    null,
                    null,
                    null
            );
        }
        return new ProfileOwnerDto(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private AsignacionDto toAsignacionDto(Asignacion asignacion) {
        return new AsignacionDto(asignacion.getId(), asignacion.getMateriaNombre(), asignacion.getCursoDescripcion());
    }

    private ProfileMateriaDto toProfileMateriaDto(Materia materia) {
        return new ProfileMateriaDto(materia.getId(), materia.getNombre(), materia.getCategoria());
    }

    private EspecialidadDto toEspecialidadDto(Especialidad especialidad) {
        return new EspecialidadDto(especialidad.getId(), especialidad.getNombre());
    }

    private ProfileGoogleClassroomCourseDto toProfileGoogleClassroomCourseDto(Course course) {
        return new ProfileGoogleClassroomCourseDto(course.getId(), course.getName(), course.getSection(), course.getRoom());
    }

    private String roleLabel(User user) {
        if (user == null) {
            return "Usuario";
        }
        return switch (user.getLevel()) {
            case 1 -> "Profesor";
            case 2 -> "Evaluador";
            case 3 -> "Administrador";
            case 4 -> "Familia";
            default -> "Usuario";
        };
    }

    private String accessDescription(User user) {
        if (user == null) {
            return "Tu perfil se muestra en modo lectura.";
        }
        return switch (user.getLevel()) {
            case 1 -> "Podés editar tus datos de contacto, usuario, especialidad y conexión con Google Classroom.";
            case 2, 3 -> "Podés editar tus datos de contacto y cuenta desde este perfil.";
            case 4 -> "Podés editar tus datos de contacto y cuenta desde este perfil.";
            default -> "Tu perfil se muestra en modo lectura.";
        };
    }

    private boolean canModifyField(String fieldName, User currentUser) {
        if (currentUser == null || fieldName == null) {
            return false;
        }
        if (currentUser.getLevel() == 3) {
            return true;
        }
        String normalized = fieldName.trim().toLowerCase();
        return !"nombre".equals(normalized)
                && !"apellido".equals(normalized)
                && !"ci".equals(normalized)
                && !"nivel".equals(normalized);
    }

    private Especialidad resolveUiEspecialidad(String specialtyId, String specialtyName, String specialtyToken) {
        if (specialtyId != null && !specialtyId.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(specialtyId);
                Especialidad especialidad = especialidadDao.findById(id);
                if (especialidad != null) {
                    return especialidad;
                }
            } catch (Exception ignored) {
                // fallback to other lookup methods
            }
        }
        if (specialtyName != null && !specialtyName.trim().isEmpty()) {
            try {
                for (Especialidad especialidad : especialidadDao.findAll()) {
                    if (especialidad.getNombre() != null && especialidad.getNombre().equalsIgnoreCase(specialtyName.trim())) {
                        return especialidad;
                    }
                }
            } catch (Exception ignored) {
                // fallback
            }
        }
        if (specialtyToken != null && !specialtyToken.trim().isEmpty()) {
            try {
                for (Especialidad especialidad : especialidadDao.findAll()) {
                    if (especialidad.getNombre() != null && ScaUiContext.normalizeSpecialty(especialidad.getNombre()).equals(specialtyToken.trim())) {
                        return especialidad;
                    }
                }
            } catch (Exception ignored) {
                // fallback
            }
        }
        return null;
    }

    private boolean saveTotpSecretForUser(User user, String totpSecret) throws SQLException {
        if (user == null) {
            return false;
        }
        if (user.getLevel() == 4) {
            Padre padre = padreDao.findById(user.getId());
            if (padre == null) {
                return false;
            }
            padre.setTotpSecret(totpSecret);
            return padreDao.update(padre);
        } else {
            Profesor profesor = profesorDao.findById(user.getId());
            if (profesor == null) {
                return false;
            }
            return profesorDao.updateTotpSecret(profesor.getId(), totpSecret);
        }
    }

    private String resolveProfesorEspecialidadNombre(Profesor profesor) {
        if (profesor == null || profesor.getEspecialidadId() == null) {
            return "Sin especialidad";
        }
        try {
            Especialidad especialidad = especialidadDao.findById(profesor.getEspecialidadId());
            if (especialidad != null && especialidad.getNombre() != null && !especialidad.getNombre().isBlank()) {
                return especialidad.getNombre();
            }
        } catch (Exception ex) {
            // ignore
        }
        return "Sin especialidad";
    }

    private List<String> parseManualSubjects(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(raw.split("[,\r\n;]+"))
                .map(token -> token == null ? "" : token.trim())
                .filter(token -> !token.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> materiaNames(List<Materia> materias) {
        if (materias == null) {
            return Collections.emptyList();
        }
        return materias.stream()
                .filter(Objects::nonNull)
                .map(Materia::getNombre)
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private boolean isPushEnabled(User user) {
        if (user == null) {
            return false;
        }
        try {
            return !pushSubscriptionDao.findByUser(user.getId(), resolveUserType(user)).isEmpty();
        } catch (SQLException ex) {
            return false;
        }
    }

    private String resolveUserType(User user) {
        if (user == null) {
            return "profesor";
        }
        return user.getLevel() == 4 ? "padre" : "profesor";
    }

    private User requireUser(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        int userLevel = ApiAuth.requireUserLevel(authentication);
        try {
            // Profesor y padre viven en tablas diferentes y sus ids pueden coincidir.
            // El rol firmado en el JWT evita resolver la cuenta desde la tabla errónea.
            User user = userDao.findByIdAndLevel(userId, userLevel);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
            }
            return user;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar el usuario", ex);
        }
    }
}
