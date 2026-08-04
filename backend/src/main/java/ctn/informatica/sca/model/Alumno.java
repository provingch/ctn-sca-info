package ctn.informatica.sca.model;

public class Alumno {
    private int id;
    private Integer ci;
    private String nombre;
    private String apellido;
    private int cursoId;
    private String correoEncargado;
    private String correoEncargado2;
    private String googleUserId;
    private String googleEmail;
    private String especialidadNombre;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getCi() {
        return ci;
    }

    public void setCi(Integer ci) {
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

    public String getCorreoEncargado() {
        return correoEncargado;
    }

    public void setCorreoEncargado(String correoEncargado) {
        this.correoEncargado = correoEncargado;
    }

    public String getCorreoEncargado2() {
        return correoEncargado2;
    }

    public void setCorreoEncargado2(String correoEncargado2) {
        this.correoEncargado2 = correoEncargado2;
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
}
