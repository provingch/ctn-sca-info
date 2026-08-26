package ctn.informatica.sca.model;

public class Sala {
    private int id;
    private String nombre;
    private Integer especialidadId;
    private String especialidadNombre;

    public Sala() {}
    public Sala(int id, String nombre, Integer especialidadId, String especialidadNombre) {
        this.id = id;
        this.nombre = nombre;
        this.especialidadId = especialidadId;
        this.especialidadNombre = especialidadNombre;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(Integer especialidadId) { this.especialidadId = especialidadId; }
    public String getEspecialidadNombre() { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre) { this.especialidadNombre = especialidadNombre; }
}
