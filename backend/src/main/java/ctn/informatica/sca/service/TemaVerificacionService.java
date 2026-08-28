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

    public VerificacionResultado verificar(int asignacionId, String temaIngresado) throws SQLException {
        int anio = AcademicPeriod.current();
        int etapa = AcademicPeriod.currentEtapa();
        // 1. resolver plan aprobado vigente para la asignacion, etapa y año actual
        Integer planId = null;
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT id FROM plan_curricular WHERE asignacion_id = ? AND etapa = ? AND estado = 'APROBADO' AND anio_lectivo = ? ORDER BY fecha_revision DESC LIMIT 1")) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, etapa);
            ps.setInt(3, anio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) planId = rs.getInt("id");
            }
        }

        if (planId == null) {
            return new VerificacionResultado("SIN_PLAN", null);
        }

        // 2. buscar próximo tema pendiente
        Integer temaId = null;
        String temasContenidos = null;
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement("SELECT id, temas_contenidos FROM tema_plan_curricular WHERE plan_curricular_id = ? AND estado_cobertura = 'PENDIENTE' ORDER BY orden_mes, bloque LIMIT 1")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    temaId = rs.getInt("id");
                    temasContenidos = rs.getString("temas_contenidos");
                }
            }
        }

        if (temaId == null) {
            // plan aprobado pero ya cubierto totalmente
            return new VerificacionResultado("OK", null);
        }

        if (coincidenTemas(temaIngresado, temasContenidos)) {
            // coincidencia suficiente -> OK, devolver tema candidato (controller marcará como cubierto)
            return new VerificacionResultado("OK", temaId);
        } else {
            return new VerificacionResultado("DUDOSO", temaId);
        }
    }
}
