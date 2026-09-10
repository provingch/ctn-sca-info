package ctn.informatica.sca.model;

public class Alumno {
    private int id;
    private String ci;
    private String nombre;
    private String apellido;
    private int cursoId;
    private String googleUserId;
    private String googleEmail;
    private String especialidadNombre;
    private Integer promocion;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // La cédula puede traer una letra (p. ej. DNI español/argentino), por eso es texto y no INT.
    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getCursoId() {
        return cursoId;
    }

    public void setCursoId(int cursoId) {
        this.cursoId = cursoId;
    }

    public String getGoogleUserId() {
        return googleUserId;
    }

    public void setGoogleUserId(String googleUserId) {
        this.googleUserId = googleUserId;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    public String getEspecialidadNombre() { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre) { this.especialidadNombre = especialidadNombre; }

    /** Año de egreso del curso al que pertenece (solo lo pobla findAllEgresados). */
    public Integer getPromocion() { return promocion; }
    public void setPromocion(Integer promocion) { this.promocion = promocion; }
}
