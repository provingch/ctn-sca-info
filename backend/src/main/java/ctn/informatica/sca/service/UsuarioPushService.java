package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.FcmTokenDao;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Push al celular del profesor para un conjunto acotado de avisos (los demás quedan solo en la
 * bandeja de Avisos). Igual que {@link ParentPushService}: nunca propaga un error al llamador, un
 * push que falla no puede romper la acción de Evaluación ni la aprobación de un plan.
 *
 * <p>Los tokens salen de {@code fcm_token} por id de usuario; la app Android los registra al iniciar
 * sesión sea cual sea el rol.
 */
@Service
public class UsuarioPushService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioPushService.class);

    /** Avisos que también llegan como push. Todo otro tipo (p. ej. PLAN_PENDIENTE) queda solo en la lista. */
    static final Set<String> TIPOS_CON_PUSH = Set.of(
            "INCUMPLIMIENTO_RESUELTO",
            "INICIAR_CLASE_BLOQUEADO",
            "PLAN_ACEPTADO",
            "PLAN_RECHAZADO");

    private final FirebaseMessagingClient fcm;
    private final FcmTokenDao tokenDao;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "usuario-push");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public UsuarioPushService(FirebaseMessagingClient fcm, FcmTokenDao tokenDao) {
        this.fcm = fcm;
        this.tokenDao = tokenDao;
    }

    /**
     * Envía el push si {@code tipo} está en la lista y el destinatario no es padre. Retorna al
     * instante; el envío corre en segundo plano.
     */
    public void notificar(int usuarioId, String userType, String tipo, String titulo, String cuerpo,
            String entidadTipo, long entidadId) {
        if (!corresponde(userType, tipo)) {
            return;
        }
        try {
            executor.submit(() -> {
                try {
                    enviar(usuarioId, tipo, titulo, cuerpo, entidadTipo, entidadId);
                } catch (Exception ex) {
                    log.warn("No se pudo enviar el push {} al usuario {}: {}", tipo, usuarioId, ex.getMessage());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("No se pudo encolar el push {} para el usuario {}: {}", tipo, usuarioId, ex.getMessage());
        }
    }

    static boolean corresponde(String userType, String tipo) {
        return tipo != null && TIPOS_CON_PUSH.contains(tipo) && !"padre".equals(userType);
    }

    /** Parte síncrona de {@link #notificar}, visible en el paquete para los tests. */
    void enviar(int usuarioId, String tipo, String titulo, String cuerpo, String entidadTipo, long entidadId)
            throws Exception {
        List<String> tokens = tokenDao.findTokensByUserId(usuarioId);
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("type", "aviso");
        data.put("tipo", tipo);
        data.put("entidadId", String.valueOf(entidadId));
        if (entidadTipo != null) {
            data.put("entidadTipo", entidadTipo);
        }
        List<String> stale = fcm.send(tokens, titulo, cuerpo, data);
        if (!stale.isEmpty()) {
            tokenDao.deleteByTokens(stale);
            log.info("Podados {} tokens FCM inválidos", stale.size());
        }
    }
}
