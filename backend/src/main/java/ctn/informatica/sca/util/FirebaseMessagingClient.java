package ctn.informatica.sca.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over the Firebase Admin SDK for sending data+notification
 * messages to the parent app. Degrades to a no-op when no credentials are
 * configured, so the backend runs fine without Firebase.
 *
 * Credentials are read, in order, from:
 *   - env FIREBASE_CREDENTIALS_JSON  (the service-account JSON, inline)
 *   - env FIREBASE_CREDENTIALS       (path to the service-account JSON file)
 *   - env GOOGLE_APPLICATION_CREDENTIALS / workload identity (SDK default)
 */
@Component
public class FirebaseMessagingClient {

    private static final Logger log = LoggerFactory.getLogger(FirebaseMessagingClient.class);
    private static final String APP_NAME = "sca-parent-fcm";

    private volatile Boolean enabled;
    private FirebaseApp app;

    public synchronized boolean isEnabled() {
        if (enabled != null) {
            return enabled;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            if (credentials == null) {
                log.info("FCM deshabilitado: no hay credenciales de Firebase configuradas");
                enabled = false;
                return false;
            }
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            this.app = FirebaseApp.getApps().stream()
                    .filter(a -> APP_NAME.equals(a.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            log.info("FCM habilitado");
            enabled = true;
        } catch (Exception ex) {
            log.warn("No se pudo inicializar Firebase; el push queda deshabilitado: {}", ex.getMessage());
            enabled = false;
        }
        return enabled;
    }

    /**
     * Sends one notification to many device tokens.
     *
     * @return the subset of {@code tokens} that FCM reported as permanently
     *         invalid (unregistered / malformed) and should be pruned.
     */
    public List<String> send(List<String> tokens, String title, String body, Map<String, String> data) {
        List<String> stale = new ArrayList<>();
        if (tokens == null || tokens.isEmpty() || !isEnabled()) {
            return stale;
        }
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);

        // FCM multicast is capped at 500 tokens per call.
        for (int start = 0; start < tokens.size(); start += 500) {
            List<String> batch = tokens.subList(start, Math.min(start + 500, tokens.size()));
            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build());
            if (data != null) {
                data.forEach(builder::putData);
            }
            try {
                BatchResponse response = messaging.sendEachForMulticast(builder.build());
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse r = responses.get(i);
                    if (r.isSuccessful()) {
                        continue;
                    }
                    FirebaseMessagingException ex = r.getException();
                    MessagingErrorCode code = ex == null ? null : ex.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        stale.add(batch.get(i));
                    } else {
                        log.debug("Fallo transitorio enviando push: {}", code);
                    }
                }
            } catch (FirebaseMessagingException ex) {
                log.warn("Error enviando lote de push FCM: {}", ex.getMessage());
            }
        }
        return stale;
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String inline = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (inline != null && !inline.isBlank()) {
            try (InputStream in = new ByteArrayInputStream(inline.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(in);
            }
        }
        String path = System.getenv("FIREBASE_CREDENTIALS");
        if (path != null && !path.isBlank()) {
            try (InputStream in = new FileInputStream(path)) {
                return GoogleCredentials.fromStream(in);
            }
        }
        try {
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException ex) {
            return null;
        }
    }
}
