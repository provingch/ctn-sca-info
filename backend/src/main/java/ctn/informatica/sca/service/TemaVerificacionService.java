package ctn.informatica.sca.service;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TemaVerificacionService extends conexion {

    protected record TemaPendiente(Integer temaId, String temasContenidos, Integer ordenMes) {
    }

    public static String normalizarTema(String tema) {
        if (tema == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(tema, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String compactado = sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compactado;
    }

    public static boolean coincidenTemas(String temaIngresado, String temaEsperado) {
        if (temaIngresado == null || temaEsperado == null) {
            return false;
        }

        String normalizadoIngresado = normalizarTema(temaIngresado);
        String normalizadoEsperado = normalizarTema(temaEsperado);
        if (normalizadoIngresado.isBlank() || normalizadoEsperado.isBlank()) {
            return false;
        }

        if (normalizadoIngresado.equals(normalizadoEsperado)) {
            return true;
        }

        String[] esperados = normalizadoEsperado.split("(?:\\s*[,;\\/]\\s*|\\s+\\&\\s+|\\s*\\|\\s*)");
        for (String esperado : esperados) {
            String valor = normalizarTema(esperado);
            if (valor.isBlank()) {
                continue;
            }
            if (normalizadoIngresado.equals(valor) || normalizadoIngresado.contains(valor) || valor.contains(normalizadoIngresado)) {
                return true;
            }
        }

        return false;
    }

    public boolean estaAtrasado(int asignacionId, String temaIngresado) throws SQLException {
        return verificar(asignacionId, temaIngresado).atrasado();
    }

    public int ordenEsperadoActual() {
        return ordenEsperadoActual(mesActual(), etapaActual());
    }

    protected int mesActual() {
        return java.time.LocalDate.now().getMonthValue();
    }

    protected int etapaActual() {
        return AcademicPeriod.currentEtapa();
    }

    protected int anioActual() {
        return AcademicPeriod.current();
    }

    protected Integer buscarPlanCurricularId(int asignacionId) throws SQLException {
        int anio = anioActual();
        int etapa = etapaActual();
        Integer planId = null;
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT id FROM plan_curricular WHERE asignacion_id = ? AND etapa = ? AND estado = 'APROBADO' AND anio_lectivo = ? ORDER BY fecha_revision DESC LIMIT 1")) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, etapa);
            ps.setInt(3, anio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    planId = rs.getInt("id");
                }
            }
        }
        return planId;
    }

    protected TemaPendiente buscarTemaPendiente(int planId) throws SQLException {
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT id, temas_contenidos, orden_mes FROM tema_plan_curricular WHERE plan_curricular_id = ? AND estado_cobertura = 'PENDIENTE' ORDER BY orden_mes, bloque LIMIT 1")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TemaPendiente(
                            rs.getInt("id"),
                            rs.getString("temas_contenidos"),
                            rs.getObject("orden_mes") == null ? null : rs.getInt("orden_mes"));
                }
            }
        }
        return null;
    }

    protected int ordenEsperadoActual(int mes, int etapa) {
        int base = etapa == 2 ? mes - 6 : mes - 2;
        int minimo = 1;
        int maximo = etapa == 2 ? 5 : 4;
        return Math.max(minimo, Math.min(base, maximo));
    }

    public int ordenTemaPendiente(int asignacionId) throws SQLException {
        Integer planId = buscarPlanCurricularId(asignacionId);
        if (planId == null) {
            return 0;
        }
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(orden_mes),0) FROM tema_plan_curricular WHERE plan_curricular_id = ? AND estado_cobertura = 'PENDIENTE'")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public VerificacionResultado verificar(int asignacionId, String temaIngresado) throws SQLException {
        Integer planId = buscarPlanCurricularId(asignacionId);
        if (planId == null) {
            return new VerificacionResultado("SIN_PLAN", null, false);
        }

        TemaPendiente temaPendiente = buscarTemaPendiente(planId);
        if (temaPendiente == null) {
            return new VerificacionResultado("OK", null, false);
        }

        boolean atrasado = temaPendiente.ordenMes() != null && temaPendiente.ordenMes() < ordenEsperadoActual();

        if (coincidenTemas(temaIngresado, temaPendiente.temasContenidos())) {
            return new VerificacionResultado("OK", temaPendiente.temaId(), atrasado);
        }

        if (atrasado) {
            return new VerificacionResultado("ATRASADO", temaPendiente.temaId(), true);
        }

        return new VerificacionResultado("DUDOSO", temaPendiente.temaId(), false);
    }
}
