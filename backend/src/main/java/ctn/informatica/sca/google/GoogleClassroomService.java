package ctn.informatica.sca.google;

import ctn.informatica.sca.config.AppConfig;
import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import java.util.LinkedHashSet;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class GoogleClassroomService {

    private static final String APPLICATION_NAME = "SCA";

    // margen de seguridad: si al access token le quedan menos de 60s, lo refrescamos antes de usarlo
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

    private GoogleClassroomService() {
        // helper only
    }

    public static boolean isGoogleConnected(Profesor profesor) {
        if (profesor == null) {
            return false;
        }
        boolean hasAccessToken = profesor.getGcAccessToken() != null && !profesor.getGcAccessToken().isBlank();
        boolean hasRefreshToken = profesor.getGcRefreshToken() != null && !profesor.getGcRefreshToken().isBlank();
        boolean hasGoogleEmail = profesor.getGoogleEmail() != null && !profesor.getGoogleEmail().isBlank();
        return hasAccessToken || hasRefreshToken || hasGoogleEmail;
    }

    public static Optional<GoogleClassroomUtils.CourseKey> parseCourseKey(String courseName) {
        return GoogleClassroomUtils.parseCourseKey(courseName);
    }

    public static Optional<GoogleClassroomUtils.CourseKey> parseCourseKey(String courseName, String room) {
        return GoogleClassroomUtils.parseCourseKey(courseName, room);
    }

    public static Optional<GoogleClassroomUtils.CourseKey> parseCourseKey(String courseName, String room, String section) {
        return GoogleClassroomUtils.parseCourseKey(courseName, room, section);
    }

    public static Classroom buildClassroomClient(Profesor profesor) throws IOException {
        HttpRequestInitializer initializer = buildCredential(profesor);
        return new Classroom.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), initializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Construye las credenciales para llamar a la API de Classroom.
     * Si el access token está vencido (o a punto de vencer) y tenemos refresh token,
     * lo renueva contra Google y persiste el nuevo token en la BD antes de continuar.
     */
    private static HttpRequestInitializer buildCredential(Profesor profesor) throws IOException {
        ensureFreshAccessToken(profesor);

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (profesor.getGcTokenExpiry() > 0 && profesor.getGcTokenExpiry() <= nowSeconds) {
            throw new IOException("Access token de Google Classroom expirado y no hay refresh token disponible. Reconecte su cuenta de Google Classroom.");
        }

        AccessToken token = new AccessToken(
                profesor.getGcAccessToken(),
                new Date(profesor.getGcTokenExpiry() * 1000));

        if (profesor.getGcRefreshToken() != null && !profesor.getGcRefreshToken().isBlank()) {
            String clientId = AppConfig.get("google.client.id");
            String clientSecret = AppConfig.get("google.client.secret");
            UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(profesor.getGcRefreshToken())
                .build();
            return new HttpCredentialsAdapter(credentials);
        }

        GoogleCredentials credentials = GoogleCredentials.create(token);
        return new HttpCredentialsAdapter(credentials);
    }

    private static void ensureFreshAccessToken(Profesor profesor) throws IOException {
        long nowSeconds = System.currentTimeMillis() / 1000;
        boolean expired = profesor.getGcTokenExpiry() > 0
                && profesor.getGcTokenExpiry() - EXPIRY_SAFETY_MARGIN_SECONDS <= nowSeconds;

        if (!expired) {
            return; // token todavía válido, nada que hacer
        }

        String refreshToken = profesor.getGcRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            // no hay forma de renovar: dejamos que la llamada falle explícitamente (401)
            // para que el profesor sepa que debe reconectar su cuenta de Google.
            System.out.println("[DEBUG] Access token vencido y no hay refresh token para profesor id=" + profesor.getId());
            return;
        }

        String clientId = AppConfig.get("google.client.id");
        String clientSecret = AppConfig.get("google.client.secret");

        GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                refreshToken,
                clientId,
                clientSecret)
                .execute();

        String newAccessToken = tokenResponse.getAccessToken();
        long newExpiry = (System.currentTimeMillis() / 1000) + tokenResponse.getExpiresInSeconds();
        // Google normalmente NO reenvía un nuevo refresh token en este flujo; conservamos el que ya teníamos
        String newRefreshToken = tokenResponse.getRefreshToken() != null
                ? tokenResponse.getRefreshToken()
                : refreshToken;

        profesor.setGcAccessToken(newAccessToken);
        profesor.setGcRefreshToken(newRefreshToken);
        profesor.setGcTokenExpiry(newExpiry);

        new ProfesorDao().updateGoogleTokens(
                profesor.getId(),
                newAccessToken,
                newRefreshToken,
                newExpiry,
                profesor.getGoogleEmail());

        System.out.println("[DEBUG] Access token de Classroom renovado para profesor id=" + profesor.getId());
    }

    public static List<Course> listTeacherCourses(Profesor profesor) throws IOException {
        if (!isGoogleConnected(profesor)) {
            return Collections.emptyList();
        }
        Classroom classroom = buildClassroomClient(profesor);
        List<Course> courses = new ArrayList<>();
        String pageToken = null;
        do {
            Classroom.Courses.List request = classroom.courses().list()
                    .setPageSize(100)
                    .setTeacherId("me");
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            ListCoursesResponse response = request.execute();
            if (response.getCourses() != null) {
                // Diagnostic: print each course returned by the API
                for (Course c : response.getCourses()) {
                    try {
                        System.out.println("[DEBUG] listTeacherCourses - courseId=" + c.getId()
                                + " name='" + c.getName() + "'" + " room='" + c.getRoom() + "'");
                    } catch (Exception e) {
                        // ignore
                    }
                }
                courses.addAll(response.getCourses());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());
        return courses;
    }

    public static boolean courseMatchesTeacherCurso(Course course, List<Curso> cursos) {
        if (course == null || cursos == null || cursos.isEmpty()) {
            return false;
        }

        String name = course.getName();
        String room = course.getRoom();
        String section = course.getSection();
        Optional<GoogleClassroomUtils.CourseKey> key = parseCourseKey(name, room, section);

        if (key.isEmpty()) {
            return false;
        }

        for (Curso curso : cursos) {
            boolean sameLevel = curso.getNivel() == key.get().getNivel();
            boolean sameSection = curso.getSeccion() != null && curso.getSeccion().equalsIgnoreCase(key.get().getSeccion());
            boolean samePeriod = curso.getPeriod() == key.get().getPeriodo();
            boolean sameSpecialty = roomStatesSpecialty(room, curso);
            if (sameLevel && sameSection && samePeriod && sameSpecialty) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convención acordada para el campo "Sala" de Google Classroom: además de
     * nivel+sección (en el nombre) y año (en la Sección de Classroom), la Sala
     * debe declarar explícitamente la ESPECIALIDAD a la que pertenece la clase
     * (ej. "Informática"). Antes esto se inferia recortando nivel+sección del
     * nombre de la clase, pero lo que queda ahí es la MATERIA puntual (ej.
     * "Algorítmica"), no la especialidad -- son conceptos distintos y casi
     * nunca coinciden como texto, lo que hacía que casi ninguna clase pasara
     * este filtro. Ahora se exige que la Sala mencione la especialidad de
     * forma explícita.
     */
    private static boolean roomStatesSpecialty(String room, Curso curso) {
        if (curso == null || room == null || room.isBlank()) {
            return false;
        }
        return GoogleClassroomUtils.containsNormalizedPhrase(room, curso.getEspecialidad());
    }

    public static List<Course> listAllowedCourses(Profesor profesor, List<Curso> cursos, List<String> teacherSubjects) throws IOException {
        List<Course> allCourses = listTeacherCourses(profesor);
        if (allCourses.isEmpty()) {
            return Collections.emptyList();
        }

        if (cursos == null || cursos.isEmpty()) {
            return allCourses;
        }

        List<String> normalizedSubjects = new ArrayList<>();
        if (teacherSubjects != null) {
            for (String subject : teacherSubjects) {
                String normalized = GoogleClassroomUtils.normalizeSubjectName(subject);
                if (!normalized.isBlank()) {
                    normalizedSubjects.add(normalized);
                }
            }
        }

        List<Course> filteredCourses = new ArrayList<>();
        LinkedHashSet<String> seenClassroomCourseIds = new LinkedHashSet<>();
        for (Course course : allCourses) {
            String name = course.getName();
            String room = course.getRoom();
            Optional<GoogleClassroomUtils.CourseKey> key = parseCourseKey(name, room, course.getSection());

            // Diagnostic: show what parsing produced for this course
            try {
                if (key.isPresent()) {
                    GoogleClassroomUtils.CourseKey k = key.get();
                    System.out.println("[DEBUG] listAllowedCourses - courseId=" + course.getId() + " parsedKey=" + k.toString());
                } else {
                    System.out.println("[DEBUG] listAllowedCourses - courseId=" + course.getId() + " parsedKey=NONE");
                }
            } catch (Exception logEx) {
                System.out.println("[DEBUG] listAllowedCourses - courseId=" + course.getId() + " parse error: " + logEx.getMessage());
            }

            if (key.isEmpty()) {
                System.out.println("[DEBUG] listAllowedCourses - courseId=" + course.getId()
                        + " name='" + name + "' room='" + course.getRoom() + "' -> no se pudo extraer nivel+sección; se descarta por ahora");
                continue;
            }

            boolean matchesAnyCurso = false;
            for (Curso curso : cursos) {
                if (courseMatchesTeacherCurso(course, List.of(curso))) {
                    matchesAnyCurso = true;
                    break;
                }
            }

            // Subject names coming from CTN are a soft hint, not a mandatory identifier.
            // Classroom course names often differ from the local subject labels, so a
            // course that already matches the selected teacher course should still be shown.
            if (!matchesAnyCurso) {
                System.out.println("[DEBUG] listAllowedCourses - courseId=" + course.getId()
                        + " name='" + name + "' room='" + course.getRoom() + "' -> no coincide con el curso seleccionado; se descarta");
                continue;
            }

            if (seenClassroomCourseIds.add(course.getId())) {
                filteredCourses.add(course);
            }
        }

        if (filteredCourses.isEmpty()) {
            return Collections.emptyList();
        }

        return filteredCourses;
    }

    public static Optional<Alumno> findBestStudentMatch(List<Alumno> alumnos, com.google.api.services.classroom.model.Student classroomStudent) {
        if (alumnos == null || alumnos.isEmpty() || classroomStudent == null) {
            return Optional.empty();
        }

        String classroomEmail = classroomStudent.getProfile() != null ? classroomStudent.getProfile().getEmailAddress() : null;
        String classroomName = classroomStudent.getProfile() != null && classroomStudent.getProfile().getName() != null
                ? classroomStudent.getProfile().getName().getFullName()
                : null;
        String normalizedName = normalizePersonName(classroomName);
        String normalizedEmail = normalizeEmail(classroomEmail);

        for (Alumno alumno : alumnos) {
            if (alumno == null) {
                continue;
            }

            String localEmail = normalizeEmail(alumno.getGoogleEmail());
            if (normalizedEmail != null && localEmail != null && normalizedEmail.equals(localEmail)) {
                return Optional.of(alumno);
            }

            String localFullName = normalizePersonName(alumno.getNombre() + " " + alumno.getApellido());
            if (normalizedName != null && localFullName != null && normalizedName.equals(localFullName)) {
                return Optional.of(alumno);
            }

            String localLastNameFirst = normalizePersonName(alumno.getApellido() + " " + alumno.getNombre());
            if (normalizedName != null && localLastNameFirst != null && normalizedName.equals(localLastNameFirst)) {
                return Optional.of(alumno);
            }
        }

        return Optional.empty();
    }

    public static int syncStudentIdentities(Profesor profesor, String courseId, List<Alumno> alumnos) throws IOException {
        if (profesor == null || courseId == null || courseId.isBlank() || alumnos == null || alumnos.isEmpty() || !isGoogleConnected(profesor)) {
            return 0;
        }

        Classroom classroom = buildClassroomClient(profesor);
        List<Student> students = new ArrayList<>();
        String pageToken = null;
        do {
            Classroom.Courses.Students.List request = classroom.courses().students().list(courseId)
                    .setPageSize(100);
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            ListStudentsResponse response = request.execute();
            if (response.getStudents() != null) {
                students.addAll(response.getStudents());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        int synced = 0;
        AlumnoDao alumnoDao = new AlumnoDao();
        for (Student student : students) {
            Optional<Alumno> match = findBestStudentMatch(alumnos, student);
            if (match.isEmpty()) {
                continue;
            }
            Alumno alumno = match.get();
            String googleUserId = student.getUserId();
            String googleEmail = student.getProfile() != null ? student.getProfile().getEmailAddress() : null;
            try {
                if (alumnoDao.updateGoogleIdentity(alumno.getId(), googleUserId, googleEmail)) {
                    synced++;
                }
            } catch (SQLException ignored) {
                // si la base no tiene las columnas, se ignora para no romper la vista
            }
        }
        return synced;
    }

    public static java.util.Map<String, Integer> linkStudentsForCourse(Profesor profesor, String courseId, List<Alumno> alumnos) throws IOException {
        if (profesor == null || courseId == null || courseId.isBlank() || alumnos == null || alumnos.isEmpty() || !isGoogleConnected(profesor)) {
            return Collections.emptyMap();
        }

        Classroom classroom = buildClassroomClient(profesor);
        java.util.Map<String, Integer> linkedStudents = new java.util.LinkedHashMap<>();
        String pageToken = null;
        do {
            Classroom.Courses.Students.List request = classroom.courses().students().list(courseId)
                    .setPageSize(100);
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            ListStudentsResponse response = request.execute();
            if (response.getStudents() != null) {
                for (Student student : response.getStudents()) {
                    Optional<Alumno> match = findBestStudentMatch(alumnos, student);
                    if (match.isEmpty()) {
                        continue;
                    }
                    Alumno alumno = match.get();
                    if (student.getUserId() != null && !student.getUserId().isBlank()) {
                        linkedStudents.put(student.getUserId(), alumno.getId());
                    }
                    try {
                        new AlumnoDao().updateGoogleIdentity(alumno.getId(), student.getUserId(), student.getProfile() != null ? student.getProfile().getEmailAddress() : null);
                    } catch (SQLException ignored) {
                        // ignore for older schemas
                    }
                }
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());
        return linkedStudents;
    }

    public static List<StudentSubmission> listStudentSubmissionsForCourseWork(Profesor profesor, String courseId, String courseWorkId) throws IOException {
        if (profesor == null || courseId == null || courseId.isBlank() || courseWorkId == null || courseWorkId.isBlank() || !isGoogleConnected(profesor)) {
            return Collections.emptyList();
        }

        Classroom classroom = buildClassroomClient(profesor);
        List<StudentSubmission> submissions = new ArrayList<>();
        String pageToken = null;
        do {
            Classroom.Courses.CourseWork.StudentSubmissions.List request = classroom.courses().courseWork().studentSubmissions().list(courseId, courseWorkId)
                    .setPageSize(100);
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            ListStudentSubmissionsResponse response = request.execute();
            if (response.getStudentSubmissions() != null) {
                submissions.addAll(response.getStudentSubmissions());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());
        return submissions;
    }

    public static Optional<Course> resolveCourseForPlanilla(Profesor profesor, Curso curso, Planilla planilla) throws IOException {
        return resolveCourseForPlanilla(profesor, curso, planilla, null);
    }

    public static Optional<Course> resolveCourseForPlanilla(Profesor profesor, Curso curso, Planilla planilla, PlanillaDao planillaDao) throws IOException {
        if (profesor == null || curso == null || planilla == null || !isGoogleConnected(profesor)) {
            return Optional.empty();
        }

        if (planilla.getGoogleCourseId() != null && !planilla.getGoogleCourseId().isBlank()) {
            try {
                Course cachedCourse = buildClassroomClient(profesor)
                        .courses()
                        .get(planilla.getGoogleCourseId())
                        .execute();
                if (cachedCourse != null) {
                    persistCourseAssociation(planilla, planillaDao, cachedCourse);
                    return Optional.of(cachedCourse);
                }
            } catch (IOException ioe) {
                System.out.println("[DEBUG] Unable to fetch Classroom course by saved google_course_id=" + planilla.getGoogleCourseId() + ": " + ioe.getMessage());
            }
        }

        Optional<Course> resolved = findCourseForPlanilla(profesor, curso, planilla);
        if (resolved.isPresent()) {
            persistCourseAssociation(planilla, planillaDao, resolved.get());
        }
        return resolved;
    }

    private static void persistCourseAssociation(Planilla planilla, PlanillaDao planillaDao, Course course) throws IOException {
        if (planilla == null || course == null || course.getId() == null || course.getId().isBlank()) {
            return;
        }
        if (planillaDao == null) {
            planillaDao = new PlanillaDao();
        }
        String courseId = course.getId();
        if (courseId.equals(planilla.getGoogleCourseId())) {
            return;
        }
        try {
            planillaDao.updateClassroomCourseId(planilla.getId(), courseId);
            planilla.setGoogleCourseId(courseId);
        } catch (SQLException sqle) {
            throw new IOException("No se pudo persistir la asociación con el curso de Google Classroom", sqle);
        }
    }

    public static List<CourseWork> listCourseWorkForCourse(Profesor profesor, String courseId) throws IOException {
        if (profesor == null || courseId == null || courseId.isBlank() || !isGoogleConnected(profesor)) {
            return Collections.emptyList();
        }

        Classroom classroom = buildClassroomClient(profesor);
        List<CourseWork> courseWorks = new ArrayList<>();
        String pageToken = null;
        do {
            Classroom.Courses.CourseWork.List request = classroom.courses().courseWork().list(courseId)
                    .setPageSize(100)
                    .setCourseWorkStates(List.of("PUBLISHED"));
            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }
            ListCourseWorkResponse response = request.execute();
            if (response.getCourseWork() != null) {
                courseWorks.addAll(response.getCourseWork());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        return courseWorks;
    }

    /**
     * Identifica a qué materia (del catálogo ya asociado al profesor) corresponde
     * un curso de Google Classroom, para poder generar su planilla al vuelo cuando
     * todavía no existe una fila en la BD.
     * <p>
     * Nota: el curso de Classroom que se recibe acá ya viene filtrado por curso
     * (nivel/sección) desde {@link #listAllowedCourses}, así que acá solo hace
     * falta desambiguar la materia, no el curso — evitando así confundir cursos
     * distintos que comparten el nombre de materia.
     * <p>
     * Si el nombre del curso calza con más de una materia candidata (ambiguo), se
     * devuelve vacío a propósito: es más seguro no crear nada que crear la planilla
     * equivocada.
     */
    public static Optional<Materia> resolveMateriaForCourse(Course course, List<Materia> candidateMaterias) {
        if (course == null || candidateMaterias == null || candidateMaterias.isEmpty()) {
            return Optional.empty();
        }

        String name = course.getName();
        String room = course.getRoom();
        Materia match = null;

        for (Materia materia : candidateMaterias) {
            if (materia == null) {
                continue;
            }
            String normalizedMateria = GoogleClassroomUtils.normalizeSubjectName(materia.getNombre());
            if (normalizedMateria.isBlank()) {
                continue;
            }
            boolean matches = GoogleClassroomUtils.containsNormalizedPhrase(name, normalizedMateria)
                    || GoogleClassroomUtils.containsNormalizedPhrase(room, normalizedMateria);
            if (matches) {
                if (match != null && match.getId() != materia.getId()) {
                    // Dos materias conocidas calzan con el mismo nombre de curso: ambiguo, no adivinamos.
                    return Optional.empty();
                }
                match = materia;
            }
        }

        return Optional.ofNullable(match);
    }

    public static Optional<Course> findCourseForPlanilla(Profesor profesor, Curso curso, Planilla planilla) throws IOException {
        if (profesor == null || curso == null || planilla == null || !isGoogleConnected(profesor)) {
            return Optional.empty();
        }

        List<Course> courses = listTeacherCourses(profesor);
        if (courses.isEmpty()) {
            return Optional.empty();
        }

        return chooseCourseFromList(courses, curso, planilla);
    }

    public static Optional<Course> chooseCourseFromList(List<Course> courses, Curso curso, Planilla planilla) {
        if (courses == null || courses.isEmpty() || curso == null || planilla == null) {
            return Optional.empty();
        }

        // Caso real: "Orientación" es una materia común, cargada en la BD una
        // vez por especialidad con el nombre sufijado (ej. "Orientación
        // Informática", "Orientación Electricidad") para poder tener una
        // planilla propia por especialidad. La clase real en Google Classroom,
        // sin embargo, se llama simplemente "Orientación" -- nadie repite el
        // nombre de la especialidad en el título de una clase común. Antes de
        // esto, esa clase nunca pasaba el filtro de especialidad (la Sala no
        // dice "Informática") y quedaba sin detectar aunque el curso
        // (nivel+sección+año) coincidiera perfecto.
        //
        // Para materias cuyo nombre en la BD YA incluye la especialidad como
        // sufijo, un match de nombre contra la clase de Classroom (usando el
        // nombre "pelado", sin el sufijo) es tan confiable como que la Sala lo
        // declare -- porque ESA planilla puntual ya está atada a una
        // especialidad concreta en la BD. Para materias sin ese sufijo (ej.
        // "Matemática", que puede repetirse igual en varias especialidades)
        // esto NO aplica: ahí seguimos dependiendo pura y exclusivamente de
        // que la Sala declare la especialidad para desambiguar
        // (ver GoogleClassroomServiceFindCourseTest.eligeLaClaseCorrectaSegunLaSalaEspecialidad).
        MateriaCore materiaCore = stripEspecialidadSuffix(planilla.getNombre(), curso.getEspecialidad());
        String normalizedMateria = GoogleClassroomUtils.normalizeSubjectName(materiaCore.core());

        List<Course> identityCandidates = new ArrayList<>();

        for (Course course : courses) {
            String name = course.getName();
            String room = course.getRoom();
            String section = course.getSection();

            Optional<GoogleClassroomUtils.CourseKey> key = parseCourseKey(name, room, section);
            if (key.isEmpty()) {
                continue;
            }
            boolean sameLevel = curso.getNivel() == key.get().getNivel();
            boolean sameSection = curso.getSeccion() != null && curso.getSeccion().equalsIgnoreCase(key.get().getSeccion());
            boolean samePeriod = curso.getPeriod() == key.get().getPeriodo();
            if (!(sameLevel && sameSection && samePeriod)) {
                continue;
            }

            boolean subjectMatches = !normalizedMateria.isBlank()
                    && (GoogleClassroomUtils.containsNormalizedPhrase(name, normalizedMateria)
                    || GoogleClassroomUtils.containsNormalizedPhrase(room, normalizedMateria));

            boolean specialtyConfirmed = roomStatesSpecialty(room, curso)
                    || (materiaCore.especialidadEraSufijo() && subjectMatches);

            if (!specialtyConfirmed) {
                continue;
            }

            if (subjectMatches) {
                return Optional.of(course);
            }
            identityCandidates.add(course);
        }

        if (identityCandidates.size() == 1) {
            return Optional.of(identityCandidates.get(0));
        }
        return Optional.empty();
    }

    private record MateriaCore(String core, boolean especialidadEraSufijo) {
    }

    /**
     * Si el nombre de la materia local termina (o empieza) con el nombre de
     * la especialidad del curso -- ej. "Orientación Informática" con
     * especialidad "Informática" -- devuelve el nombre "pelado" (sin ese
     * sufijo/prefijo) junto con especialidadEraSufijo=true. Si no encuentra
     * ese patrón, devuelve el nombre sin modificar y especialidadEraSufijo=false.
     */
    private static MateriaCore stripEspecialidadSuffix(String materiaNombre, String especialidad) {
        if (materiaNombre == null || materiaNombre.isBlank() || especialidad == null || especialidad.isBlank()) {
            return new MateriaCore(materiaNombre, false);
        }
        String normalizedMateria = GoogleClassroomUtils.normalizeSubjectName(materiaNombre);
        String normalizedEspecialidad = GoogleClassroomUtils.normalizeSubjectName(especialidad);
        if (normalizedEspecialidad.isBlank() || normalizedMateria.equals(normalizedEspecialidad)) {
            return new MateriaCore(materiaNombre, false);
        }

        String suffix = " " + normalizedEspecialidad;
        if (normalizedMateria.endsWith(suffix)) {
            String core = normalizedMateria.substring(0, normalizedMateria.length() - suffix.length()).trim();
            return new MateriaCore(core, !core.isBlank());
        }
        String prefix = normalizedEspecialidad + " ";
        if (normalizedMateria.startsWith(prefix)) {
            String core = normalizedMateria.substring(prefix.length()).trim();
            return new MateriaCore(core, !core.isBlank());
        }
        return new MateriaCore(materiaNombre, false);
    }

    // tryExtractLevel is now unused after removing the weak fallback logic.
    // It is kept commented for reference but will be removed in a future cleanup.

    private static String normalizePersonName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return GoogleClassroomUtils.normalizeSubjectName(value).replaceAll("\\s+", " ").trim();
    }

    

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}