/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.model;

import java.time.LocalDate;

/**
 *
 * @author jonat
 */
public class Tarea {

    private int id;
    private int planillaId;
    private int instrumentoId;
    private LocalDate fecha;
    private int total;
    private String titulo;
    private String googleCourseworkId;
    private String googleCourseworkUrl;
    private LocalDate fechaInicio;
    private LocalDate fechaLimite;

    public Tarea() {
    }
    
    public Tarea(int id, int planillaId, int instrumentoId, LocalDate fecha, int total, String titulo) {
        this.id = id;
        this.planillaId = planillaId;
        this.instrumentoId = instrumentoId;
        this.fecha = fecha;
        this.total = total;
        this.titulo = titulo;
    }

    // getters
    public int getId() {
        return id;
    }

    public int getPlanillaId() {
        return planillaId;
    }

    public int getInstrumentoId() {
        return instrumentoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getTotal() {
        return total;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getGoogleCourseworkId() {
        return googleCourseworkId;
    }

    public String getGoogleCourseworkUrl() {
        return googleCourseworkUrl;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public String getTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tarea: ").append(titulo != null ? titulo : "Sin título");
        if (fechaInicio != null) {
            sb.append("\nInicio: ").append(fechaInicio);
        }
        if (fechaLimite != null) {
            sb.append("\nLímite: ").append(fechaLimite);
        }
        if (googleCourseworkUrl != null && !googleCourseworkUrl.isBlank()) {
            sb.append("\nAbrir: ").append(googleCourseworkUrl);
        }
        return sb.toString();
    }

    public static int resolveEtapaIndexByPublicationDate(LocalDate publicationDate) {
        if (publicationDate == null) {
            return 1;
        }
        LocalDate transition = LocalDate.of(publicationDate.getYear(), 7, 15);
        return publicationDate.isBefore(transition) ? 1 : 2;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPlanillaId(int planillaId) {
        this.planillaId = planillaId;
    }

    public void setInstrumentoId(int instrumentoId) {
        this.instrumentoId = instrumentoId;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setGoogleCourseworkId(String googleCourseworkId) {
        this.googleCourseworkId = googleCourseworkId;
    }

    public void setGoogleCourseworkUrl(String googleCourseworkUrl) {
        this.googleCourseworkUrl = googleCourseworkUrl;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

}
