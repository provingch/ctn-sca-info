package ctn.informatica.sca.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * IP real del cliente detrás de nginx. El backend corre en la misma máquina que el proxy (deploy.sh hace
 * {@code proxy_pass http://127.0.0.1:PUERTO}) y nginx manda dos headers:
 * <ul>
 *   <li>{@code X-Real-IP $remote_addr}: nginx lo <b>pisa</b> con la dirección de la conexión, el cliente no lo controla;</li>
 *   <li>{@code X-Forwarded-For $proxy_add_x_forwarded_for}: nginx <b>agrega</b> la dirección de la conexión al final de lo
 *       que el cliente haya mandado. El primer elemento es del cliente (falsificable); el último es el de nginx.</li>
 * </ul>
 * Por eso se usa X-Real-IP y, si falta, el <b>último</b> elemento de X-Forwarded-For, nunca el primero. Los headers sólo
 * se creen cuando la conexión viene del propio proxy (loopback): si alguien le pega directo al puerto del backend, sus
 * headers no valen y se registra la dirección de su conexión. Todo se valida como IP para que un header no pueda meter
 * texto en el registro de actividad (una línea por acción).
 */
public final class RequestIpUtil {

    public static final String DESCONOCIDA = "desconocida";
    private static final Pattern FORMA_DE_IP = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private RequestIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return DESCONOCIDA;
        }
        String conexion = ipValida(request.getRemoteAddr());
        if (esLoopback(conexion)) {
            String real = ipValida(request.getHeader("X-Real-IP"));
            if (real != null) {
                return real;
            }
            String reenviada = request.getHeader("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                String[] partes = reenviada.split(",");
                String ultima = ipValida(partes[partes.length - 1]);
                if (ultima != null) {
                    return ultima;
                }
            }
        }
        return conexion != null ? conexion : DESCONOCIDA;
    }

    private static boolean esLoopback(String ip) {
        return ip != null && (ip.startsWith("127.") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1"));
    }

    private static String ipValida(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return FORMA_DE_IP.matcher(limpio).matches() ? limpio : null;
    }
}
