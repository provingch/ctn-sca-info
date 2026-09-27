package ctn.informatica.sca.integration.gema.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la integración con GEMA ({@code gema.integration.*}). Sin {@code api-key}
 * configurada la integración queda deshabilitada: {@link GemaApiKeyFilter} responde 503 a todo
 * {@code /api/integracion/**} sin siquiera mirar la clave recibida.
 */
@Component
@ConfigurationProperties(prefix = "gema.integration")
public class GemaProperties {

    private String clientId = "gema";
    private String apiKey = "";
    private String allowedIps = "";

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** IPs permitidas (vacío = sin restricción de origen). */
    public List<String> allowedIpList() {
        if (allowedIps == null || allowedIps.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .toList();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }
}
