package mc.mrd_og.redbug;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ProbeManager {

    public static final int MAX_PROBES_PER_PLAYER = 16;

    private static final Color[] PARTICLE_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.AQUA, Color.FUCHSIA, Color.ORANGE, Color.WHITE
    };

    private static final String[] HEX_COLORS = {
            "#ff5555", "#5577ff", "#55ff55", "#ffff55",
            "#55ffff", "#ff55ff", "#ffaa00", "#ffffff"
    };

    private final Redbug plugin;
    private final Map<UUID, List<Probe>> playerProbes = new HashMap<>();
    private final Map<UUID, Integer> probeModeScope = new HashMap<>();
    private final Map<Integer, BusConfig> busConfigs = new HashMap<>();

    private volatile String latestJson = "{\"channels\":{}}";

    private VcdWriter vcdWriter;
    private BukkitTask samplingTask;
    private BukkitTask particleTask;

    public ProbeManager(Redbug plugin) {
        this.plugin = plugin;
    }

    public void start() {
        samplingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (List<Probe> probes : playerProbes.values()) {
                    for (Probe probe : probes) {
                        probe.sample();
                    }
                }
                latestJson = buildJson();

                if (vcdWriter != null && !vcdWriter.isClosed()) {
                    try {
                        vcdWriter.writeSample();
                    } catch (IOException e) {
                        plugin.getLogger().warning("VCD write error: " + e.getMessage());
                        stopVcd();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, List<Probe>> entry : playerProbes.entrySet()) {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) continue;

                    List<Probe> probes = entry.getValue();
                    Map<Integer, Integer> scopeCounters = new HashMap<>();
                    for (Probe probe : probes) {
                        int idx = scopeCounters.merge(probe.getScope(), 0, (a, b) -> a + 1);
                        Location loc = probe.getLocation().clone().add(0.5, 1.2, 0.5);
                        if (!loc.isChunkLoaded()) continue;

                        float size = 0.5f + (probe.getCurrentLevel() / 15f) * 1.5f;
                        Color particleColor = PARTICLE_COLORS[idx % PARTICLE_COLORS.length];
                        var dustOptions = new Particle.DustOptions(particleColor, size);
                        player.spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, dustOptions);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void stop() {
        stopVcd();
        if (samplingTask != null) samplingTask.cancel();
        if (particleTask != null) particleTask.cancel();
    }

    // --- VCD recording ---

    public Path startVcd(String filename) throws IOException {
        stopVcd();
        Map<Integer, List<Probe>> allScopes = collectAllScopeProbes();
        if (allScopes.isEmpty()) {
            throw new IOException("No probes to record");
        }
        Path path = Path.of(filename).toAbsolutePath();
        vcdWriter = new VcdWriter(path, allScopes, busConfigs);
        return path;
    }

    public void stopVcd() {
        if (vcdWriter != null) {
            try { vcdWriter.close(); } catch (IOException ignored) {}
            vcdWriter = null;
        }
    }

    public boolean isRecordingVcd() {
        return vcdWriter != null && !vcdWriter.isClosed();
    }

    private Map<Integer, List<Probe>> collectAllScopeProbes() {
        Map<Integer, List<Probe>> result = new TreeMap<>();
        for (List<Probe> probes : playerProbes.values()) {
            for (Probe probe : probes) {
                result.computeIfAbsent(probe.getScope(), k -> new ArrayList<>()).add(probe);
            }
        }
        return result;
    }

    // --- Probe mode ---

    public int toggleProbeMode(UUID uuid, int scope) {
        Integer current = probeModeScope.get(uuid);
        if (current != null && current == scope) {
            probeModeScope.remove(uuid);
            return 0;
        }
        probeModeScope.put(uuid, scope);
        return scope;
    }

    public boolean isInProbeMode(UUID uuid) {
        return probeModeScope.containsKey(uuid);
    }

    public int getProbeModeScope(UUID uuid) {
        return probeModeScope.getOrDefault(uuid, 1);
    }

    // --- Probe management ---

    public AddResult addProbe(UUID uuid, Location location, int scope) {
        List<Probe> probes = playerProbes.computeIfAbsent(uuid, k -> new ArrayList<>());

        for (int i = 0; i < probes.size(); i++) {
            if (probes.get(i).isAt(location)) {
                probes.remove(i);
                return AddResult.REMOVED;
            }
        }

        if (probes.size() >= MAX_PROBES_PER_PLAYER) {
            return AddResult.FULL;
        }

        probes.add(new Probe(location, scope));
        return AddResult.ADDED;
    }

    public List<Probe> getProbes(UUID uuid) {
        return playerProbes.getOrDefault(uuid, Collections.emptyList());
    }

    public Map<Integer, List<Probe>> getProbesGrouped(UUID uuid) {
        Map<Integer, List<Probe>> grouped = new TreeMap<>();
        for (Probe probe : getProbes(uuid)) {
            grouped.computeIfAbsent(probe.getScope(), k -> new ArrayList<>()).add(probe);
        }
        return grouped;
    }

    public void clearProbes(UUID uuid) {
        playerProbes.remove(uuid);
        probeModeScope.remove(uuid);
    }

    public void clearScope(UUID uuid, int scope) {
        List<Probe> probes = playerProbes.get(uuid);
        if (probes != null) {
            probes.removeIf(p -> p.getScope() == scope);
            if (probes.isEmpty()) playerProbes.remove(uuid);
        }
    }

    public void cleanupPlayer(UUID uuid) {
        probeModeScope.remove(uuid);
    }

    // --- Bus config ---

    public BusConfig getBusConfig(int scope) {
        return busConfigs.get(scope);
    }

    public BusConfig getOrCreateBusConfig(int scope) {
        return busConfigs.computeIfAbsent(scope, k -> new BusConfig());
    }

    public void removeBusConfig(int scope) {
        busConfigs.remove(scope);
    }

    // --- JSON ---

    public String getLatestJson() {
        return latestJson;
    }

    private String buildJson() {
        // Collect all probes by scope across all players
        Map<Integer, List<Object[]>> scopeProbes = new TreeMap<>();

        for (Map.Entry<UUID, List<Probe>> entry : playerProbes.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            String playerName = player != null ? player.getName() : "Unknown";

            for (Probe probe : entry.getValue()) {
                scopeProbes.computeIfAbsent(probe.getScope(), k -> new ArrayList<>())
                        .add(new Object[]{playerName, probe});
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"channels\":{");

        boolean firstScope = true;
        for (Map.Entry<Integer, List<Object[]>> entry : scopeProbes.entrySet()) {
            if (!firstScope) sb.append(',');
            firstScope = false;

            int scope = entry.getKey();
            List<Object[]> entries = entry.getValue();

            sb.append('"').append(scope).append("\":{\"probes\":[");

            // Build probe JSON entries
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) sb.append(',');
                String pName = (String) entries.get(i)[0];
                Probe probe = (Probe) entries.get(i)[1];
                appendProbeJson(sb, pName, probe, i);
            }
            sb.append(']');

            // Bus config for this scope
            BusConfig bus = busConfigs.get(scope);
            if (bus != null) {
                List<Probe> rawProbes = entries.stream().map(e -> (Probe) e[1]).toList();
                List<Probe> sorted = bus.sortProbes(rawProbes);
                int bitCount = sorted.size();
                long currentValue = bus.computeValue(sorted);
                long maxValue = (1L << bitCount) - 1;
                long[] history = bus.computeHistory(sorted);

                sb.append(",\"bus\":{");
                sb.append("\"sort\":\"").append(bus.getSort().getId()).append('"');
                sb.append(",\"format\":\"").append(bus.getFormat().getId()).append('"');
                sb.append(",\"wordSize\":").append(bus.getWordSize());
                sb.append(",\"bitCount\":").append(bitCount);
                sb.append(",\"decimal\":").append(currentValue);
                sb.append(",\"maxValue\":").append(maxValue);
                sb.append(",\"display\":\"").append(escapeJson(bus.formatValue(currentValue, bitCount))).append('"');
                sb.append(",\"bits\":\"").append(bus.toBitsString(currentValue, bitCount)).append('"');
                sb.append(",\"history\":[");
                for (int i = 0; i < history.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(history[i]);
                }
                sb.append("]}");
            }

            sb.append('}');
        }

        sb.append("}}");
        return sb.toString();
    }

    private void appendProbeJson(StringBuilder sb, String playerName, Probe probe, int colorIndex) {
        sb.append("{\"player\":\"").append(escapeJson(playerName)).append('"');
        sb.append(",\"index\":").append(colorIndex + 1);
        sb.append(",\"color\":\"").append(HEX_COLORS[colorIndex % HEX_COLORS.length]).append('"');
        sb.append(",\"location\":\"").append(probe.coordString()).append('"');
        sb.append(",\"level\":").append(probe.getCurrentLevel());
        sb.append(",\"history\":[");

        boolean first = true;
        for (int level : probe.getHistory()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(level);
        }
        sb.append("]}");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public enum AddResult {
        ADDED, REMOVED, FULL
    }
}
