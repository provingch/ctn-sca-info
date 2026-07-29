package ctn.informatica.sca.model;

import java.sql.Timestamp;

public class RasgoAsistencia {

    private int id;
    private int planillaRasgoId;
    private int alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private String alumnoEmail;
    private String estado;
    private Timestamp respondedAt;
    private String tema;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlanillaRasgoId() {
        return planillaRasgoId;
    }

    public void setPlanillaRasgoId(int planillaRasgoId) {
        this.planillaRasgoId = planillaRasgoId;
    }

    public int getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(int alumnoId) {
        this.alumnoId = alumnoId;
    }

    public String getAlumnoNombre() {
        return alumnoNombre;
    }

    public void setAlumnoNombre(String alumnoNombre) {
        this.alumnoNombre = alumnoNombre;
    }

    public String getAlumnoApellido() {
        return alumnoApellido;
    }

    public void setAlumnoApellido(String alumnoApellido) {
        this.alumnoApellido = alumnoApellido;
    }

    public String getAlumnoEmail() {
        return alumnoEmail;
    }

    public void setAlumnoEmail(String alumnoEmail) {
        this.alumnoEmail = alumnoEmail;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getAlumnoNombreCompleto() {
        String nombre = alumnoNombre == null ? "" : alumnoNombre.trim();
        String apellido = alumnoApellido == null ? "" : alumnoApellido.trim();
        if (apellido.isEmpty()) {
            return nombre;
        }
        if (nombre.isEmpty()) {
            return apellido;
        }
        return nombre + " " + apellido;
    }
}
