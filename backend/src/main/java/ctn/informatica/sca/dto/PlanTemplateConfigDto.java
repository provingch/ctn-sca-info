package ctn.informatica.sca.dto;

import java.util.ArrayList;
import java.util.List;

/** Configuración que el profesor elige antes de descargar la plantilla. */
public class PlanTemplateConfigDto {
    public String etapa;
    public List<MesConfig> meses = new ArrayList<>();
    public RsaConfig rsa;

    public static class MesConfig {
        public String mes;
        public int bloques;
    }

    public static class RsaConfig {
        public String etapas;
        public int puntos;
        public int toleranciaFaltas;
    }
}
