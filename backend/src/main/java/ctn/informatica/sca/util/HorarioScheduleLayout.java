package ctn.informatica.sca.util;

import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class HorarioScheduleLayout {

    static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
    static final String ETIQUETA_MANANA = "M";

    private HorarioScheduleLayout() {
    }

    static List<BlockLayout> buildBlocks(List<HoraCatedra> horas, List<HorarioSlot> slots, int dayCount) {
        List<HoraCatedra> manana = new ArrayList<>();
        List<HoraCatedra> tarde = new ArrayList<>();
        for (HoraCatedra hora : horas == null ? Collections.<HoraCatedra>emptyList() : horas) {
            if (hora == null) {
                continue;
            }
            if (ETIQUETA_MANANA.equalsIgnoreCase(hora.getEtiqueta())) {
                manana.add(hora);
            } else {
                tarde.add(hora);
            }
        }

        Map<Integer, List<HorarioSlot>> slotsPorHora = new LinkedHashMap<>();
        for (HorarioSlot slot : slots == null ? Collections.<HorarioSlot>emptyList() : slots) {
            if (slot == null) {
                continue;
            }
            slotsPorHora.computeIfAbsent(slot.getHoraCatedraId(), key -> new ArrayList<>()).add(slot);
        }

        List<BlockLayout> blocks = new ArrayList<>();
        BlockLayout mananaBlock = buildBlock("MAÑANA", manana, slotsPorHora, dayCount);
        if (!mananaBlock.rows.isEmpty()) {
            blocks.add(mananaBlock);
        }
        BlockLayout tardeBlock = buildBlock("T.O.", tarde, slotsPorHora, dayCount);
        if (!tardeBlock.rows.isEmpty()) {
            blocks.add(tardeBlock);
        }
        return blocks;
    }

    private static BlockLayout buildBlock(String name, List<HoraCatedra> horasBloque, Map<Integer, List<HorarioSlot>> slotsPorHora, int dayCount) {
        BlockLayout block = new BlockLayout(name, dayCount);
        if (horasBloque == null || horasBloque.isEmpty()) {
            return block;
        }

        for (int i = 0; i < horasBloque.size(); i++) {
            HoraCatedra hora = horasBloque.get(i);
            RowLayout row = RowLayout.hour(hora, buildHoraLabel(i, hora));
            for (int dia = 1; dia <= dayCount; dia++) {
                HorarioSlot slot = findSlot(slotsPorHora, hora.getId(), dia);
                if (slot != null) {
                    row.cells.put(dia, new CellLayout(slot, buildSlotText(slot)));
                    if (slot.getSalaNombre() != null && !slot.getSalaNombre().isBlank()) {
                        block.salasPorDia.get(dia).add(slot.getSalaNombre());
                    }
                } else {
                    row.cells.put(dia, CellLayout.empty());
                }
            }
            block.rows.add(row);

            boolean hayHuecoDespues = i + 1 < horasBloque.size()
                    && !areConsecutive(hora, horasBloque.get(i + 1));
            if (hayHuecoDespues) {
                block.rows.add(RowLayout.receso());
            }
        }

        block.mergedRanges.addAll(computeMergedRanges(block.rows));
        return block;
    }

    private static List<MergedRange> computeMergedRanges(List<RowLayout> rows) {
        List<MergedRange> ranges = new ArrayList<>();
        for (int day = 1; day <= 6; day++) {
            int rowIndex = 0;
            while (rowIndex < rows.size()) {
                RowLayout current = rows.get(rowIndex);
                if (current.receso) {
                    rowIndex++;
                    continue;
                }
                CellLayout cell = current.cells.get(day);
                if (cell == null || cell.slot == null) {
                    rowIndex++;
                    continue;
                }

                int end = rowIndex;
                while (end + 1 < rows.size()) {
                    RowLayout next = rows.get(end + 1);
                    if (next.receso) {
                        break;
                    }
                    CellLayout nextCell = next.cells.get(day);
                    if (nextCell == null || nextCell.slot == null) {
                        break;
                    }
                    if (nextCell.slot.getAsignacionId() != cell.slot.getAsignacionId()) {
                        break;
                    }
                    if (!areConsecutive(current.hora, next.hora)) {
                        break;
                    }
                    end++;
                    current = next;
                }

                if (end > rowIndex) {
                    ranges.add(new MergedRange(day, rowIndex, end, cell.slot));
                }
                rowIndex = end + 1;
            }
        }
        return ranges;
    }

    private static boolean areConsecutive(HoraCatedra first, HoraCatedra second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getHoraFin() == null || second.getHoraInicio() == null) {
            return true;
        }
        return Objects.equals(first.getHoraFin(), second.getHoraInicio());
    }

    private static HorarioSlot findSlot(Map<Integer, List<HorarioSlot>> slotsPorHora, int horaCatedraId, int dia) {
        List<HorarioSlot> candidatos = slotsPorHora.get(horaCatedraId);
        if (candidatos == null) {
            return null;
        }
        for (HorarioSlot slot : candidatos) {
            if (slot != null && slot.getDiaSemana() == dia) {
                return slot;
            }
        }
        return null;
    }

    static String buildSlotText(HorarioSlot slot) {
        if (slot == null) {
            return "";
        }
        String materia = slot.getMateriaNombre() == null ? "" : slot.getMateriaNombre().trim();
        String profesor = slot.getProfesorNombre() == null ? "" : slot.getProfesorNombre().trim();
        String texto = materia.isBlank() ? profesor : (profesor.isBlank() ? materia : materia + "\n" + profesor);
        if (slot.getSalaNombre() != null && !slot.getSalaNombre().isBlank()) {
            texto += "\nSala: " + slot.getSalaNombre().trim();
        }
        return texto;
    }

    private static String buildHoraLabel(int index, HoraCatedra hora) {
        String numero = hora != null && hora.getNumero() > 0 ? String.valueOf(hora.getNumero()) : String.valueOf(index + 1);
        String inicio = hora != null && hora.getHoraInicio() != null ? hora.getHoraInicio().toString() : "";
        String fin = hora != null && hora.getHoraFin() != null ? hora.getHoraFin().toString() : "";
        return numero + "° " + inicio + " - " + fin;
    }

    static boolean isManana(HoraCatedra hora) {
        return hora != null && ETIQUETA_MANANA.equalsIgnoreCase(hora.getEtiqueta());
    }

    static final class BlockLayout {
        final String name;
        final int dayCount;
        final List<RowLayout> rows = new ArrayList<>();
        final List<MergedRange> mergedRanges = new ArrayList<>();
        final Map<Integer, Set<String>> salasPorDia = new LinkedHashMap<>();

        BlockLayout(String name, int dayCount) {
            this.name = name;
            this.dayCount = dayCount;
            for (int dia = 1; dia <= dayCount; dia++) {
                salasPorDia.put(dia, new LinkedHashSet<>());
            }
        }
    }

    static final class RowLayout {
        final boolean receso;
        final HoraCatedra hora;
        final String horaLabel;
        final Map<Integer, CellLayout> cells = new LinkedHashMap<>();

        private RowLayout(boolean receso, HoraCatedra hora, String horaLabel) {
            this.receso = receso;
            this.hora = hora;
            this.horaLabel = horaLabel;
        }

        static RowLayout hour(HoraCatedra hora, String horaLabel) {
            return new RowLayout(false, hora, horaLabel);
        }

        static RowLayout receso() {
            return new RowLayout(true, null, "RECESO");
        }
    }

    static final class CellLayout {
        final HorarioSlot slot;
        final String text;

        private CellLayout(HorarioSlot slot, String text) {
            this.slot = slot;
            this.text = text;
        }

        static CellLayout empty() {
            return new CellLayout(null, "");
        }
    }

    static final class MergedRange {
        final int day;
        final int startRow;
        final int endRow;
        final HorarioSlot slot;

        MergedRange(int day, int startRow, int endRow, HorarioSlot slot) {
            this.day = day;
            this.startRow = startRow;
            this.endRow = endRow;
            this.slot = slot;
        }
    }
}
