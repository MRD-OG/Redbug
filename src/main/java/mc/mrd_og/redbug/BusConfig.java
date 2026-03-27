package mc.mrd_og.redbug;

import java.util.*;

public class BusConfig {

    public enum SortStrategy {
        ADD_ORDER("add", "Addition order (first added = LSB)"),
        ADD_REVERSE("add_rev", "Reverse add order (first added = MSB)"),
        Y_ASC("y_asc", "Y ascending (lowest Y = LSB)"),
        Y_DESC("y_desc", "Y descending (highest Y = LSB)"),
        X_ASC("x_asc", "X ascending (lowest X = LSB)"),
        X_DESC("x_desc", "X descending (highest X = LSB)"),
        Z_ASC("z_asc", "Z ascending (lowest Z = LSB)"),
        Z_DESC("z_desc", "Z descending (highest Z = LSB)");

        private final String id;
        private final String description;

        SortStrategy(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDescription() { return description; }

        public static SortStrategy fromId(String id) {
            for (SortStrategy s : values()) {
                if (s.id.equalsIgnoreCase(id)) return s;
            }
            return null;
        }

        public static List<String> ids() {
            return Arrays.stream(values()).map(SortStrategy::getId).toList();
        }
    }

    public enum DisplayFormat {
        BINARY("bin", "Binary"),
        DECIMAL("dec", "Decimal"),
        HEX("hex", "Hexadecimal");

        private final String id;
        private final String description;

        DisplayFormat(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDescription() { return description; }

        public static DisplayFormat fromId(String id) {
            for (DisplayFormat f : values()) {
                if (f.id.equalsIgnoreCase(id)) return f;
            }
            return null;
        }

        public static List<String> ids() {
            return Arrays.stream(values()).map(DisplayFormat::getId).toList();
        }
    }

    private SortStrategy sort = SortStrategy.ADD_ORDER;
    private DisplayFormat format = DisplayFormat.BINARY;
    private int wordSize = 8;

    public SortStrategy getSort() { return sort; }
    public void setSort(SortStrategy sort) { this.sort = sort; }
    public DisplayFormat getFormat() { return format; }
    public void setFormat(DisplayFormat format) { this.format = format; }
    public int getWordSize() { return wordSize; }
    public void setWordSize(int wordSize) { this.wordSize = Math.max(1, Math.min(wordSize, 32)); }

    public List<Probe> sortProbes(List<Probe> probes) {
        List<Probe> sorted = new ArrayList<>(probes);
        switch (sort) {
            case ADD_ORDER -> { /* natural order */ }
            case ADD_REVERSE -> Collections.reverse(sorted);
            case Y_ASC -> sorted.sort(Comparator.comparingInt(p -> p.getLocation().getBlockY()));
            case Y_DESC -> sorted.sort(Comparator.comparingInt((Probe p) -> p.getLocation().getBlockY()).reversed());
            case X_ASC -> sorted.sort(Comparator.comparingInt(p -> p.getLocation().getBlockX()));
            case X_DESC -> sorted.sort(Comparator.comparingInt((Probe p) -> p.getLocation().getBlockX()).reversed());
            case Z_ASC -> sorted.sort(Comparator.comparingInt(p -> p.getLocation().getBlockZ()));
            case Z_DESC -> sorted.sort(Comparator.comparingInt((Probe p) -> p.getLocation().getBlockZ()).reversed());
        }
        return sorted;
    }

    public long computeValue(List<Probe> sortedProbes) {
        long value = 0;
        for (int i = 0; i < sortedProbes.size(); i++) {
            if (sortedProbes.get(i).getCurrentLevel() > 0) {
                value |= (1L << i);
            }
        }
        return value;
    }

    /**
     * Compute the bus value at each time step from all probe histories.
     * Returns an array of decimal values, aligned to the longest history.
     */
    public long[] computeHistory(List<Probe> sortedProbes) {
        int maxLen = 0;
        int[][] arrays = new int[sortedProbes.size()][];
        for (int i = 0; i < sortedProbes.size(); i++) {
            LinkedList<Integer> h = sortedProbes.get(i).getHistory();
            arrays[i] = new int[h.size()];
            int j = 0;
            for (int v : h) arrays[i][j++] = v;
            maxLen = Math.max(maxLen, arrays[i].length);
        }

        long[] history = new long[maxLen];
        for (int t = 0; t < maxLen; t++) {
            long value = 0;
            for (int bit = 0; bit < arrays.length; bit++) {
                int offset = maxLen - arrays[bit].length;
                int idx = t - offset;
                int level = (idx >= 0 && idx < arrays[bit].length) ? arrays[bit][idx] : 0;
                if (level > 0) {
                    value |= (1L << bit);
                }
            }
            history[t] = value;
        }
        return history;
    }

    public String formatValue(long value, int bitCount) {
        return switch (format) {
            case BINARY -> formatBinary(value, bitCount);
            case DECIMAL -> String.valueOf(value);
            case HEX -> formatHex(value, bitCount);
        };
    }

    public String formatBinary(long value, int bitCount) {
        StringBuilder sb = new StringBuilder("0b");
        for (int i = bitCount - 1; i >= 0; i--) {
            sb.append((value >> i) & 1);
            if (i > 0 && i % 4 == 0) sb.append('_');
        }
        return sb.toString();
    }

    public String toBitsString(long value, int bitCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = bitCount - 1; i >= 0; i--) {
            sb.append((value >> i) & 1);
        }
        return sb.toString();
    }

    private String formatHex(long value, int bitCount) {
        if (bitCount <= wordSize) {
            int hexDigits = (bitCount + 3) / 4;
            return "0x" + padHex(value, hexDigits);
        }
        StringBuilder sb = new StringBuilder();
        int numWords = (bitCount + wordSize - 1) / wordSize;
        long mask = (1L << wordSize) - 1;
        int hexDigits = (wordSize + 3) / 4;
        for (int w = numWords - 1; w >= 0; w--) {
            long word = (value >> (w * wordSize)) & mask;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append("0x").append(padHex(word, hexDigits));
        }
        return sb.toString();
    }

    private static String padHex(long value, int digits) {
        String hex = Long.toHexString(value).toUpperCase();
        return "0".repeat(Math.max(0, digits - hex.length())) + hex;
    }
}
