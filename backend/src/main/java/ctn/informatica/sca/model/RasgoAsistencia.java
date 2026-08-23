package ctn.informatica.sca.model;

import java.sql.Timestamp;
import java.util.List;

public class RasgoAsistencia {

    private int id;
    private int planillaRasgoId;
    private int alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private String alumnoEmail;
    private String estado;
    private String faltaCodigo;
    private String faltaObservacion;
    private Timestamp respondedAt;
    private String tema;
    private List<String> codigos = List.of();

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

    public String getFaltaCodigo() {
        return faltaCodigo;
    }

    public void setFaltaCodigo(String faltaCodigo) {
        this.faltaCodigo = faltaCodigo;
    }

    public String getFaltaObservacion() {
        return faltaObservacion;
    }

    public void setFaltaObservacion(String faltaObservacion) {
        this.faltaObservacion = faltaObservacion;
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

    public List<String> getCodigos() {
        return codigos;
    }

    public void setCodigos(List<String> codigos) {
        this.codigos = codigos == null ? List.of() : codigos;
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

    public String getCodigoDescripcion() {
        if (faltaCodigo == null) {
            return "Sin código asignado";
        }
        return switch (faltaCodigo) {
            case "N1" -> "Sale del aula sin autorización";
            case "N2" -> "No realiza la tarea asignada en clase";
            case "N3" -> "No dispone de los materiales necesarios";
            case "N4" -> "No presenta las tareas asignadas para la casa";
            case "N5" -> "Utiliza vocabulario indebido en clase";
            case "N6" -> "Charla mucho en clase";
            case "N7" -> "No utiliza el uniforme establecido";
            case "N8" -> "Ausente en clase, presente en la Institución";
            default -> "Sin código asignado";
        };
    }
}
