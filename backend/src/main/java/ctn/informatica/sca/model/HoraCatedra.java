package ctn.informatica.sca.model;

import java.time.LocalTime;

public class HoraCatedra {

    private int id;
    private int numero;
    private String etiqueta;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public HoraCatedra() {
    }

    public HoraCatedra(int id, int numero, String etiqueta, LocalTime horaInicio, LocalTime horaFin) {
        this.id = id;
        this.numero = numero;
        this.etiqueta = etiqueta;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }
}
