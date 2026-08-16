package ctn.informatica.sca.model;

import java.time.LocalDate;

public class ParentTaskGrade {
    public static final String CALIFICADA = "CALIFICADA";
    public static final String ENTREGADA_PENDIENTE = "ENTREGADA_PENDIENTE";
    public static final String NO_ENTREGADA = "NO_ENTREGADA";
    public static final String PENDIENTE = "PENDIENTE";

    private int tareaId;
    private String tareaTitulo;
    private LocalDate fecha;
    private int total;
    private Integer puntos;
    private String etapa;
    private int planillaId;
    private String estado;

    public int getTareaId() { return tareaId; }
    public void setTareaId(int tareaId) { this.tareaId = tareaId; }
    public String getTareaTitulo() { return tareaTitulo; }
    public void setTareaTitulo(String tareaTitulo) { this.tareaTitulo = tareaTitulo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    public String getEtapa() { return etapa; }
    public void setEtapa(String etapa) { this.etapa = etapa; }
    public int getPlanillaId() { return planillaId; }
    public void setPlanillaId(int planillaId) { this.planillaId = planillaId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public static String resolveEstado(boolean tienePuntaje, Integer puntos, boolean tareaClassroom, LocalDate fechaLimite, LocalDate hoy) {
        if (tienePuntaje && puntos != null) {
            return CALIFICADA;
        }
        if (tienePuntaje && tareaClassroom) {
            return ENTREGADA_PENDIENTE;
        }
        if (fechaLimite != null && hoy != null && fechaLimite.isBefore(hoy)) {
            return NO_ENTREGADA;
        }
        return PENDIENTE;
    }
}
