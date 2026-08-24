package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.UserDao;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);
    private static final DateTimeFormatter LINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${sca.activity-logs.dir:./data/activity-logs}")
    private String baseDir = "./data/activity-logs";

    private final UserDao userDao;

    public ActivityLogService() {
        this(new UserDao());
    }

    public ActivityLogService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void registrar(int usuarioId, String accion) throws IOException, SQLException {
        if (usuarioId <= 0) {
            return;
        }
        String accionNormalizada = accion == null ? "" : accion.trim();
        if (accionNormalizada.isEmpty()) {
            return;
        }

        Path directory = Paths.get(baseDir == null || baseDir.isBlank() ? "./data/activity-logs" : baseDir);
        Files.createDirectories(directory);

        Path path = resolveUserLogPath(usuarioId);
        String linea = "[" + LocalDateTime.now().format(LINE_FORMATTER) + "] " + accionNormalizada;
        Files.write(path,
                List.of(linea),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        String storedPath = userDao.findActivityLogPathById(usuarioId);
        if (storedPath == null || storedPath.isBlank()) {
            String relativePath = "usuario-" + usuarioId + ".txt";
            userDao.updateActivityLogPath(usuarioId, relativePath);
        }
    }

    public List<String> leerUltimas(int usuarioId, int limite) throws IOException, SQLException {
        if (usuarioId <= 0 || limite <= 0) {
            return Collections.emptyList();
        }

        String relativePath = userDao.findActivityLogPathById(usuarioId);
        if (relativePath == null || relativePath.isBlank()) {
            return Collections.emptyList();
        }

        Path path = Paths.get(baseDir == null || baseDir.isBlank() ? "./data/activity-logs" : baseDir).resolve(relativePath).normalize();
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        int start = Math.max(0, lines.size() - limite);
        List<String> result = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            result.add(lines.get(i));
        }
        return result;
    }

    public long getTotalBytes() {
        try {
            Path directory = Paths.get(baseDir == null || baseDir.isBlank() ? "./data/activity-logs" : baseDir);
            if (!Files.exists(directory)) {
                return 0L;
            }
            long total = 0L;
            try (var stream = Files.list(directory)) {
                for (var path : (Iterable<Path>) stream::iterator) {
                    if (path.getFileName() != null && path.getFileName().toString().toLowerCase().endsWith(".txt") && Files.isRegularFile(path)) {
                        total += Files.size(path);
                    }
                }
            }
            return total;
        } catch (IOException ex) {
            log.warn("No se pudo calcular el espacio ocupado por los logs de actividad", ex);
            return 0L;
        }
    }

    public Path resolveUserLogPath(int usuarioId) {
        return Paths.get(baseDir == null || baseDir.isBlank() ? "./data/activity-logs" : baseDir)
                .resolve("usuario-" + usuarioId + ".txt")
                .normalize();
    }
}
