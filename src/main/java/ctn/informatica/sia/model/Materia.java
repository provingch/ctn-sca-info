package ctn.informatica.sia.model;

import java.util.ArrayList;
import java.util.List;

public class Materia {

    private int id;
    private String nombre;
    private String categoria; // "comun" o "especifico"
    private List<Integer> especialidadIds = new ArrayList<>();

    public Materia(int id, String nombre, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public List<Integer> getEspecialidadIds() {
        return especialidadIds;
    }

    public void setEspecialidadIds(List<Integer> especialidadIds) {
        this.especialidadIds = especialidadIds == null ? new ArrayList<>() : especialidadIds;
    }

    public boolean isComun() {
        return "comun".equalsIgnoreCase(categoria);
    }

    @Override
    public String toString() {
        return nombre;
    }
}