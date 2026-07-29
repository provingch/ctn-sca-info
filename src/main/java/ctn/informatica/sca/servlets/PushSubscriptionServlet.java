package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.PushSubscriptionDao;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.PushNotificationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "PushSubscriptionServlet", urlPatterns = {"/PushSubscriptionServlet"})
public class PushSubscriptionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, false, "Sesión no válida.");
            return;
        }

        String action = req.getParameter("action");
        String endpoint = req.getParameter("endpoint");
        String p256dh = req.getParameter("p256dh");
        String auth = req.getParameter("auth");

        if ("save".equals(action)) {
            try {
                boolean saved = new PushSubscriptionDao().save(user.getId(), resolveUserType(user), endpoint, p256dh, auth);
                if (saved) {
                    writeJson(resp, true, "Suscripción guardada.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    writeJson(resp, false, "No se pudo guardar la suscripción.");
                }
            } catch (SQLException ex) {
                log("Error saving push subscription", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, false, "No se pudo guardar la suscripción.");
            }
            return;
        }

        if ("unsubscribe".equals(action)) {
            try {
                boolean deleted = new PushSubscriptionDao().deleteByUser(user.getId(), resolveUserType(user));
                writeJson(resp, true, deleted ? "Suscripción eliminada." : "No había suscripciones para eliminar.");
            } catch (SQLException ex) {
                log("Error deleting push subscriptions", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, false, "No se pudo eliminar la suscripción.");
            }
            return;
        }

        if ("test".equals(action)) {
            try {
                boolean delivered = PushNotificationService.sendToUser(user.getId(), resolveUserType(user), "Prueba CTN", "Esta es una notificación de prueba.", req.getContextPath() + "/ProfileServlet");
                int subscriptionCount = new PushSubscriptionDao().findByUser(user.getId(), resolveUserType(user)).size();
                boolean hasVapidKeys = !PushNotificationService.resolveVapidPublicKey().isBlank() && !PushNotificationService.resolveVapidPrivateKey().isBlank();
                writeJson(resp, true, PushNotificationService.buildDeliveryMessage(delivered, subscriptionCount, hasVapidKeys));
            } catch (SQLException ex) {
                log("Error sending push test", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, false, "No se pudo enviar la prueba.");
            }
            return;
        }

        writeJson(resp, false, "Acción no permitida.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, false, "Sesión no válida.");
            return;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("publicKey", PushNotificationService.resolveVapidPublicKey());
            payload.put("subscribed", !new PushSubscriptionDao().findByUser(user.getId(), resolveUserType(user)).isEmpty());
            writeJson(resp, true, payload);
        } catch (SQLException ex) {
            log("Error loading push state", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "No se pudo cargar el estado de push.");
        }
    }

    private String resolveUserType(User user) {
        if (user == null) {
            return "profesor";
        }
        return switch (user.getLevel()) {
            case 4 -> "padre";
            default -> "profesor";
        };
    }

    private void writeJson(HttpServletResponse resp, boolean success, String message) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"success\":" + success + ",\"message\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }

    private void writeJson(HttpServletResponse resp, boolean success, Map<String, Object> payload) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        StringBuilder builder = new StringBuilder();
        builder.append("{\"success\":").append(success).append(",\"data\":{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append('"').append(entry.getKey()).append('"').append(':');
            Object value = entry.getValue();
            if (value instanceof Boolean booleanValue) {
                builder.append(booleanValue);
            } else if (value instanceof Number || value instanceof String) {
                builder.append('"').append(String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            } else {
                builder.append('"').append(String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
        }
        builder.append("}}");
        resp.getWriter().write(builder.toString());
    }
}
