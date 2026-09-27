package ctn.informatica.sca.integration.gema.dto;

import java.util.List;

/**
 * Envoltorio de listado para {@code /api/integracion/gema/**} (ver convenciones del plan de
 * integración): {@code data} nunca es {@code null} — sin resultados es {@code []}, nunca 404.
 */
public record PageResponse<T>(List<T> data, int total, int page, int size) {

    private static final int MAX_SIZE = 500;
    private static final int DEFAULT_SIZE = 50;

    public static <T> PageResponse<T> of(List<T> all, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to), all.size(), safePage, safeSize);
    }
}
