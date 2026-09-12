package ctn.informatica.sca.dto;

import java.util.ArrayList;
import java.util.List;

/** Configuración que el profesor elige antes de descargar la plantilla. */
public class PlanTemplateConfigDto {
    public String etapa;
    public List<MesConfig> meses = new ArrayList<>();

    public static class MesConfig {
        public String mes;
        public int bloques;
    }
}
