package ctn.informatica.sia.model;

import java.time.LocalDate;

public class ParentTaskGrade {
    private int tareaId;
    private String tareaTitulo;
    private LocalDate fecha;
    private int total;
    private int puntos;
    private String etapa;
    private int planillaId;

    public int getTareaId() { return tareaId; }
    public void setTareaId(int tareaId) { this.tareaId = tareaId; }
    public String getTareaTitulo() { return tareaTitulo; }
    public void setTareaTitulo(String tareaTitulo) { this.tareaTitulo = tareaTitulo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public String getEtapa() { return etapa; }
    public void setEtapa(String etapa) { this.etapa = etapa; }
    public int getPlanillaId() { return planillaId; }
    public void setPlanillaId(int planillaId) { this.planillaId = planillaId; }
}
