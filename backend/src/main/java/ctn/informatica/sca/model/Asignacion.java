package ctn.informatica.sca.model;

/**
 * Simple DTO/model for an assignment of a professor to a materia in a specific curso.
 */
public class Asignacion {

    private int id;
    private int profesorId;
    private int materiaId;
    private int cursoId;

    // Display helpers
    private String profesorNombre;
    private String materiaNombre;
    private String cursoDescripcion;
    // New explicit fields for UI: especialidad (nombre), nivel/promocion, seccion
    private String especialidad;
    private Integer cursoNivel; // promocion
    private String cursoSeccion;
    
    // Estado del plan curricular para esta asignación
    private String estadoPlan; // PENDIENTE, APROBADO, RECHAZADO, NO_CARGADO

    public Asignacion() {}

    public Asignacion(int id, int profesorId, int materiaId, int cursoId) {
        this.id = id;
        this.profesorId = profesorId;
        this.materiaId = materiaId;
        this.cursoId = cursoId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProfesorId() { return profesorId; }
    public void setProfesorId(int profesorId) { this.profesorId = profesorId; }

    public int getMateriaId() { return materiaId; }
    public void setMateriaId(int materiaId) { this.materiaId = materiaId; }

    public int getCursoId() { return cursoId; }
    public void setCursoId(int cursoId) { this.cursoId = cursoId; }

    public String getProfesorNombre() { return profesorNombre; }
    public void setProfesorNombre(String profesorNombre) { this.profesorNombre = profesorNombre; }

    public String getMateriaNombre() { return materiaNombre; }
    public void setMateriaNombre(String materiaNombre) { this.materiaNombre = materiaNombre; }

    public String getCursoDescripcion() { return cursoDescripcion; }
    public void setCursoDescripcion(String cursoDescripcion) { this.cursoDescripcion = cursoDescripcion; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public Integer getCursoNivel() { return cursoNivel; }
    public void setCursoNivel(Integer cursoNivel) { this.cursoNivel = cursoNivel; }

    public String getCursoSeccion() { return cursoSeccion; }
    public void setCursoSeccion(String cursoSeccion) { this.cursoSeccion = cursoSeccion; }

    public String getEstadoPlan() { return estadoPlan; }
    public void setEstadoPlan(String estadoPlan) { this.estadoPlan = estadoPlan; }

}
