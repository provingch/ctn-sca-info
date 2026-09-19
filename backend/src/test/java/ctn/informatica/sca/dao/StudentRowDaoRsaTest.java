package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.util.AcademicPeriod;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StudentRowDaoRsaTest {

    private static final int CURSO_ID = 7;
    private static final int PROFESOR_ID = 3;

    private final Connection connection = mock(Connection.class);
    private final StudentRowDao dao = new StudentRowDao() {
        @Override
        public Connection getCon() {
            return connection;
        }
    };

    private static Planilla planilla(String etapa, Integer rsaPuntos) {
        Planilla p = new Planilla(50, CURSO_ID, 1, "comun", "Laboratorio Java", 2026, etapa, PROFESOR_ID);
        p.setRsaPuntos(rsaPuntos);
        if (rsaPuntos != null) {
            p.setRsaToleranciaValor(new BigDecimal("10"));
            p.setRsaToleranciaUnidad("PORCENTAJE");
        }
        return p;
    }

    @Test
    void etapaDateRange_coincideConElCriterioDeEtapaDeLasTareas() {
        for (int etapa = 1; etapa <= 2; etapa++) {
            LocalDate[] r = StudentRowDao.etapaDateRange(2026, etapa);
            assertEquals(etapa, AcademicPeriod.etapaAt(r[0]), "El inicio del rango pertenece a la etapa " + etapa);
            assertEquals(etapa, AcademicPeriod.etapaAt(r[1].minusDays(1)), "El último día del rango pertenece a la etapa " + etapa);
            assertTrue(AcademicPeriod.etapaAt(r[1]) != etapa,
                    "El límite superior (exclusivo) ya no es de la etapa " + etapa);
        }
        assertEquals(LocalDate.of(2026, 1, 1), StudentRowDao.etapaDateRange(2026, 1)[0]);
        assertEquals(LocalDate.of(2026, 6, 22), StudentRowDao.etapaDateRange(2026, 1)[1]);
        assertEquals(LocalDate.of(2026, 6, 22), StudentRowDao.etapaDateRange(2026, 2)[0]);
        assertEquals(LocalDate.of(2027, 1, 1), StudentRowDao.etapaDateRange(2026, 2)[1]);
    }

    @Test
    void countFaltasPorAlumno_agrupaEnUnaSolaConsultaConCursoProfesorYRangoDeEtapa() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT ra.alumno_id"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt("alumno_id")).thenReturn(11, 12);
        when(rs.getInt("faltas")).thenReturn(5, 1);

        Map<Integer, Integer> faltas = dao.countFaltasPorAlumno(planilla("segunda", 10));

        assertEquals(Map.of(11, 5, 12, 1), faltas);
        verify(ps).setInt(1, CURSO_ID);
        verify(ps).setInt(2, PROFESOR_ID);
        verify(ps).setDate(3, Date.valueOf(LocalDate.of(2026, 6, 22)));
        verify(ps).setDate(4, Date.valueOf(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void countClasesDadas_cuentaClasesDistintasEnElRangoDeLaEtapa() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT COUNT(DISTINCT pr.id)"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(20);

        assertEquals(20, dao.countClasesDadas(planilla("primera", 10)));

        verify(ps).setInt(1, CURSO_ID);
        verify(ps).setInt(2, PROFESOR_ID);
        verify(ps).setDate(3, Date.valueOf(LocalDate.of(2026, 1, 1)));
        verify(ps).setDate(4, Date.valueOf(LocalDate.of(2026, 6, 22)));
    }

    @Test
    void loadRowsForPlanilla_conRsa_sumaElPuntajeRsaAntesDePorcentajeYNota() throws Exception {
        stubRegistros(2);
        PreparedStatement psFaltas = mock(PreparedStatement.class);
        ResultSet rsFaltas = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT ra.alumno_id"))).thenReturn(psFaltas);
        when(psFaltas.executeQuery()).thenReturn(rsFaltas);
        when(rsFaltas.next()).thenReturn(true, false);
        when(rsFaltas.getInt("alumno_id")).thenReturn(101);
        when(rsFaltas.getInt("faltas")).thenReturn(5);
        PreparedStatement psClases = mock(PreparedStatement.class);
        ResultSet rsClases = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT COUNT(DISTINCT pr.id)"))).thenReturn(psClases);
        when(psClases.executeQuery()).thenReturn(rsClases);
        when(rsClases.next()).thenReturn(true);
        when(rsClases.getInt(1)).thenReturn(20);

        // RSA de 10 puntos, tolerancia 10% de 20 clases = 2 faltas; TP = 0 de tareas + 10 de RSA
        Planilla planilla = planilla("primera", 10);
        planilla.computeGradeRanges(10);
        List<StudentRow> rows = dao.loadRowsForPlanilla(planilla, Map.of(), 10);

        assertEquals(2, rows.size());
        StudentRow conFaltas = rows.get(0);
        assertEquals(101, conFaltas.getAlumnoId());
        assertEquals(7, conFaltas.getRsaPuntos(), "5 faltas - 2 de tolerancia = 3 de descuento");
        assertEquals(7, conFaltas.getTotal());
        assertEquals(70, conFaltas.getPorcentaje());
        StudentRow sinFaltas = rows.get(1);
        assertEquals(10, sinFaltas.getRsaPuntos());
        assertEquals(10, sinFaltas.getTotal());
        assertEquals(100, sinFaltas.getPorcentaje());
        assertEquals(5, sinFaltas.getNota(), "El puntaje máximo (TP ampliado) da nota 5");
    }

    @Test
    void loadRowsForPlanilla_sinRsa_noConsultaFaltasNiClasesYNoAlteraElTotal() throws Exception {
        stubRegistros(1);

        Planilla planilla = planilla("primera", null);
        planilla.computeGradeRanges(10);
        List<StudentRow> rows = dao.loadRowsForPlanilla(planilla, Map.of(1, 10), 10);

        assertEquals(1, rows.size());
        assertEquals(0, rows.get(0).getRsaPuntos());
        assertEquals(0, rows.get(0).getTotal());
        verify(connection, never()).prepareStatement(startsWith("SELECT ra.alumno_id"));
        verify(connection, never()).prepareStatement(startsWith("SELECT COUNT(DISTINCT pr.id)"));
    }

    /** Alumnos 101, 102, ... sin puntajes cargados, del curso de la planilla. */
    private void stubRegistros(int alumnos) throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT r.id AS registro_id"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        Boolean[] nexts = new Boolean[alumnos];
        Integer[] registroIds = new Integer[alumnos];
        Integer[] alumnoIds = new Integer[alumnos];
        for (int i = 0; i < alumnos; i++) {
            nexts[i] = true;
            registroIds[i] = 900 + i;
            alumnoIds[i] = 101 + i;
        }
        Boolean[] rest = new Boolean[alumnos];
        System.arraycopy(nexts, 1, rest, 0, alumnos - 1);
        rest[alumnos - 1] = false;
        when(rs.next()).thenReturn(nexts[0], rest);
        when(rs.getInt("registro_id")).thenReturn(registroIds[0], java.util.Arrays.copyOfRange(registroIds, 1, alumnos));
        when(rs.getInt("alumno_id")).thenReturn(alumnoIds[0], java.util.Arrays.copyOfRange(alumnoIds, 1, alumnos));
        when(rs.getInt("curso_id")).thenReturn(CURSO_ID);
        when(rs.getString("nombre")).thenReturn("Ana");
        when(rs.getString("apellido")).thenReturn("Perez");
        when(rs.getInt("tarea_id")).thenReturn(0);
    }
}
