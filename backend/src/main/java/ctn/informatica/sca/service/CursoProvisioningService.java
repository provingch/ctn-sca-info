package ctn.informatica.sca.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.PushNotificationService;

@Service
public class CursoProvisioningService {
    private static final Logger log = LoggerFactory.getLogger(CursoProvisioningService.class);

    private final CursoDao cursoDao;
    private final EspecialidadDao especialidadDao;
    private final ProfesorDao profesorDao;

    public CursoProvisioningService() {
        this.cursoDao = new CursoDao();
        this.especialidadDao = new EspecialidadDao();
        this.profesorDao = new ProfesorDao();
    }

    public CursoProvisioningService(CursoDao cursoDao, EspecialidadDao especialidadDao) {
        this(cursoDao, especialidadDao, new ProfesorDao());
    }

    public CursoProvisioningService(CursoDao cursoDao, EspecialidadDao especialidadDao, ProfesorDao profesorDao) {
        this.cursoDao = cursoDao;
        this.especialidadDao = especialidadDao;
        this.profesorDao = profesorDao;
    }

    public record CreatedCourse(int especialidadId, String especialidadNombre, int promocion, String seccion) {}
    public record ProvisioningResult(List<CreatedCourse> created, List<Especialidad> omitted) {}

    public ProvisioningResult ensureCursosForPeriod() throws Exception {
        return ensureCursosForPeriod(null);
    }

    public ProvisioningResult ensureCursosForPeriod(Integer specialtyFilter) throws Exception {
        int target = ctn.informatica.sca.util.AcademicPeriod.current() + 2;
        List<CreatedCourse> created = new ArrayList<>();
        List<Especialidad> omitted = new ArrayList<>();

        List<Especialidad> especialidades = especialidadDao.findAll();
        for (Especialidad e : especialidades) {
            if (specialtyFilter != null && !specialtyFilter.equals(e.getId())) continue;
            Set<String> secciones = cursoDao.listDistinctSeccionesForEspecialidad(e.getId());
            if (secciones == null || secciones.isEmpty()) {
                omitted.add(e);
                continue;
            }
            int createdBeforeEspecialidad = created.size();
            for (String s : secciones) {
                if (s == null) continue;
                if (!cursoDao.existsCurso(e.getId(), target, s)) {
                    boolean inserted = cursoDao.createCursoIfNotExists(e.getId(), target, s);
                    if (inserted) {
                        created.add(new CreatedCourse(e.getId(), e.getNombre(), target, s));
                    }
                }
            }
            if (created.size() > createdBeforeEspecialidad) {
                for (Profesor admin : profesorDao.findAdminsByEspecialidadId(e.getId())) {
                    if (admin.getNivel() == 3) {
                        PushNotificationService.sendToUser(
                                admin.getId(),
                                "profesor",
                                "Curso 1er año creado",
                                "Ya se creó el curso de " + e.getNombre() + " para " + target
                                        + " — cargá los ingresantes cuando tengas la lista.",
                                "/admin/alumnos");
                    }
                }
            }
        }
        return new ProvisioningResult(created, omitted);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledProvision() {
        try {
            ProvisioningResult r = ensureCursosForPeriod();
            if (!r.created().isEmpty()) {
                log.info("Curso provisioning: created {} courses", r.created().size());
            }
            if (!r.omitted().isEmpty()) {
                log.warn("Curso provisioning: omitted {} especialidades with no precedent", r.omitted().size());
            }
        } catch (Exception ex) {
            log.error("Error provisioning cursos: {}", ex.getMessage(), ex);
        }
    }
}
