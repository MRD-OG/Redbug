package mc.mrd_og.redbug.deprecated;

import mc.mrd_og.redbug.deprecated.BusConfig;
import mc.mrd_og.redbug.deprecated.Probe;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Writes probe data in IEEE VCD (Value Change Dump) format.
 * GTKWave can open the file while it's being written for live viewing.
 */
public class VcdWriter implements Closeable {

    private record ScopeEntry(int scopeId, List<Probe> probes, BusConfig.SortStrategy busSort, int busBits) {}

    private final BufferedWriter writer;
    private final List<ScopeEntry> scopeEntries = new ArrayList<>();
    private final Map<Probe, Character> probeIds = new LinkedHashMap<>();
    private final Map<Integer, Character> busIds = new HashMap<>();
    private final Map<Character, String> lastValues = new HashMap<>();
    private long timestamp = 0;
    private boolean closed = false;

    public VcdWriter(Path filePath, Map<Integer, List<Probe>> scopeProbes, Map<Integer, BusConfig> busConfigs) throws IOException {
        writer = new BufferedWriter(new FileWriter(filePath.toFile()));

        char nextId = '!';
        for (Map.Entry<Integer, List<Probe>> entry : new TreeMap<>(scopeProbes).entrySet()) {
            int scopeId = entry.getKey();
            List<Probe> probes = entry.getValue();
            BusConfig bus = busConfigs.get(scopeId);
            BusConfig.SortStrategy sort = bus != null ? bus.getSort() : null;

            scopeEntries.add(new ScopeEntry(scopeId, probes, sort, probes.size()));

            for (Probe p : probes) {
                probeIds.put(p, nextId++);
                if (nextId == '"') nextId++; // skip quote to avoid confusion
            }
            if (sort != null && !probes.isEmpty()) {
                busIds.put(scopeId, nextId++);
                if (nextId == '"') nextId++;
            }
        }

        writeHeader();
        writeInitialDump();
    }

    private void writeHeader() throws IOException {
        writer.write("$version Redbug Oscilloscope $end\n");
        writer.write("$date " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " $end\n");
        writer.write("$timescale 100ms $end\n");
        writer.write("$scope module redbug $end\n");

        for (ScopeEntry entry : scopeEntries) {
            writer.write("$scope module scope_" + entry.scopeId + " $end\n");

            int idx = 1;
            for (Probe probe : entry.probes) {
                char id = probeIds.get(probe);
                String name = "p" + idx + "_" + probe.coordString()
                        .replace("(", "").replace(")", "").replace(", ", "_");
                writer.write("$var wire 1 " + id + " " + name + " $end\n");
                idx++;
            }

            if (busIds.containsKey(entry.scopeId)) {
                char busId = busIds.get(entry.scopeId);
                writer.write("$var reg " + entry.busBits + " " + busId + " bus[" + (entry.busBits - 1) + ":0] $end\n");
            }

            writer.write("$upscope $end\n");
        }

        writer.write("$upscope $end\n");
        writer.write("$enddefinitions $end\n");
        writer.flush();
    }

    private void writeInitialDump() throws IOException {
        writer.write("#0\n");
        writer.write("$dumpvars\n");

        for (ScopeEntry entry : scopeEntries) {
            for (Probe probe : entry.probes) {
                char id = probeIds.get(probe);
                String val = probe.getCurrentLevel() > 0 ? "1" : "0";
                writer.write(val + id + "\n");
                lastValues.put(id, val);
            }

            if (entry.busSort != null && busIds.containsKey(entry.scopeId)) {
                char busId = busIds.get(entry.scopeId);
                String binStr = computeBusBinary(entry);
                writer.write("b" + binStr + " " + busId + "\n");
                lastValues.put(busId, binStr);
            }
        }

        writer.write("$end\n");
        writer.flush();
    }

    /**
     * Called on each sampling tick. Writes value changes since last sample.
     */
    public synchronized void writeSample() throws IOException {
        if (closed) return;

        timestamp++;
        StringBuilder changes = new StringBuilder();
        boolean anyChange = false;

        for (ScopeEntry entry : scopeEntries) {
            for (Probe probe : entry.probes) {
                char id = probeIds.get(probe);
                String val = probe.getCurrentLevel() > 0 ? "1" : "0";
                if (!val.equals(lastValues.get(id))) {
                    changes.append(val).append(id).append('\n');
                    lastValues.put(id, val);
                    anyChange = true;
                }
            }

            if (entry.busSort != null && busIds.containsKey(entry.scopeId)) {
                char busId = busIds.get(entry.scopeId);
                String binStr = computeBusBinary(entry);
                if (!binStr.equals(lastValues.get(busId))) {
                    changes.append('b').append(binStr).append(' ').append(busId).append('\n');
                    lastValues.put(busId, binStr);
                    anyChange = true;
                }
            }
        }

        if (anyChange) {
            writer.write("#" + timestamp + "\n");
            writer.write(changes.toString());
            writer.flush();
        }
    }

    private String computeBusBinary(ScopeEntry entry) {
        // Sort probes according to the bus sort strategy
        List<Probe> sorted = sortProbes(entry.probes, entry.busSort);
        long value = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getCurrentLevel() > 0) {
                value |= (1L << i);
            }
        }
        return toBinaryString(value, sorted.size());
    }

    private List<Probe> sortProbes(List<Probe> probes, BusConfig.SortStrategy sort) {
        List<Probe> sorted = new ArrayList<>(probes);
        switch (sort) {
            case ADD_ORDER -> { /* natural */ }
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

    private static String toBinaryString(long value, int bits) {
        StringBuilder sb = new StringBuilder(bits);
        for (int i = bits - 1; i >= 0; i--) {
            sb.append((value >> i) & 1);
        }
        return sb.toString();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            writer.flush();
            writer.close();
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
