package ctn.informatica.sca.model;

public class CursoBase {

    private int id;
    private int especialidadId;
    private String especialidad;
    private int nivel;
    private String seccion;

    public CursoBase() {
    }

    public CursoBase(int id, int especialidadId, String especialidad, int nivel, String seccion) {
        this.id = id;
        this.especialidadId = especialidadId;
        this.especialidad = especialidad;
        this.nivel = nivel;
        this.seccion = seccion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(int especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public String getCursoOrdinal() {
        return switch (nivel) {
            case 1 -> "1º";
            case 2 -> "2º";
            case 3 -> "3º";
            default -> "Desconocido";
        };
    }

    @Override
    public String toString() {
        return especialidad + " " + getCursoOrdinal() + " Sección: " + seccion;
    }
}
