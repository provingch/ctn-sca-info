package ctn.informatica.sca.model;

public class HorarioSlot {

    private int id;
    private int asignacionId;
    private int usuarioId;
    private int cursoId;
    private int diaSemana;
    private int horaCatedraId;
    private String sala;

    private String materiaNombre;
    private String cursoDescripcion;
    private String profesorNombre;
    private Integer horaCatedraNumero;
    private String horaCatedraEtiqueta;
    private String horaInicio;
    private String horaFin;

    public HorarioSlot() {
    }

    public HorarioSlot(int id, int asignacionId, int usuarioId, int cursoId, int diaSemana, int horaCatedraId, String sala) {
        this.id = id;
        this.asignacionId = asignacionId;
        this.usuarioId = usuarioId;
        this.cursoId = cursoId;
        this.diaSemana = diaSemana;
        this.horaCatedraId = horaCatedraId;
        this.sala = sala;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAsignacionId() {
        return asignacionId;
    }

    public void setAsignacionId(int asignacionId) {
        this.asignacionId = asignacionId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getCursoId() {
        return cursoId;
    }

    public void setCursoId(int cursoId) {
        this.cursoId = cursoId;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public int getHoraCatedraId() {
        return horaCatedraId;
    }

    public void setHoraCatedraId(int horaCatedraId) {
        this.horaCatedraId = horaCatedraId;
    }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public String getMateriaNombre() {
        return materiaNombre;
    }

    public void setMateriaNombre(String materiaNombre) {
        this.materiaNombre = materiaNombre;
    }

    public String getCursoDescripcion() {
        return cursoDescripcion;
    }

    public void setCursoDescripcion(String cursoDescripcion) {
        this.cursoDescripcion = cursoDescripcion;
    }

    public String getProfesorNombre() {
        return profesorNombre;
    }

    public void setProfesorNombre(String profesorNombre) {
        this.profesorNombre = profesorNombre;
    }

    public Integer getHoraCatedraNumero() {
        return horaCatedraNumero;
    }

    public void setHoraCatedraNumero(Integer horaCatedraNumero) {
        this.horaCatedraNumero = horaCatedraNumero;
    }

    public String getHoraCatedraEtiqueta() {
        return horaCatedraEtiqueta;
    }

    public void setHoraCatedraEtiqueta(String horaCatedraEtiqueta) {
        this.horaCatedraEtiqueta = horaCatedraEtiqueta;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }
}
