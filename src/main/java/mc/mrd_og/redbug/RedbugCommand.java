package mc.mrd_og.redbug;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class RedbugCommand implements BasicCommand {

    private static final List<String> SUBCOMMANDS = List.of("probe", "bus", "vcd", "view", "list", "clear", "help");
    private static final List<String> BUS_ACTIONS = List.of("sort", "format", "info", "reset");

    private final ProbeManager probeManager;

    public RedbugCommand(ProbeManager probeManager) {
        this.probeManager = probeManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "probe" -> handleProbe(player, args);
            case "bus" -> handleBus(player, args);
            case "vcd" -> handleVcd(player, args);
            case "view" -> handleView(player);
            case "list" -> handleList(player);
            case "clear" -> handleClear(player, args);
            case "help" -> showHelp(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use /rb help", NamedTextColor.RED));
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }

        String sub = args[0].toLowerCase();

        if ((sub.equals("probe") || sub.equals("clear")) && args.length == 2) {
            return List.of("1", "2", "3", "4").stream()
                    .filter(s -> s.startsWith(args[1])).toList();
        }

        if (sub.equals("vcd") && args.length == 2) {
            return List.of("start", "stop").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }

        if (sub.equals("bus")) {
            if (args.length == 2) {
                return List.of("1", "2", "3", "4").stream()
                        .filter(s -> s.startsWith(args[1])).toList();
            }
            if (args.length == 3) {
                return BUS_ACTIONS.stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
            }
            if (args.length == 4) {
                String action = args[2].toLowerCase();
                if (action.equals("sort")) {
                    return BusConfig.SortStrategy.ids().stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase())).toList();
                }
                if (action.equals("format")) {
                    return BusConfig.DisplayFormat.ids().stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase())).toList();
                }
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("format") && args[3].equalsIgnoreCase("hex")) {
                return List.of("4", "8", "16").stream()
                        .filter(s -> s.startsWith(args[4])).toList();
            }
        }

        return List.of();
    }

    private void handleProbe(Player player, String[] args) {
        int scope = 1;
        if (args.length >= 2) {
            try {
                scope = Integer.parseInt(args[1]);
                if (scope < 1) scope = 1;
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid scope number.", NamedTextColor.RED));
                return;
            }
        }

        int result = probeManager.toggleProbeMode(player.getUniqueId(), scope);
        if (result > 0) {
            player.sendMessage(Component.text("\u26a1 Probe mode ", NamedTextColor.GREEN)
                    .append(Component.text("ENABLED", NamedTextColor.AQUA))
                    .append(Component.text(" for scope " + result + ". Right-click blocks to place/remove probes.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("\u26a1 Probe mode ", NamedTextColor.YELLOW)
                    .append(Component.text("DISABLED", NamedTextColor.RED)));
        }
    }

    private void handleVcd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /rb vcd <start [filename]|stop>", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "start" -> {
                if (probeManager.isRecordingVcd()) {
                    player.sendMessage(Component.text("Already recording. Use /rb vcd stop first.", NamedTextColor.RED));
                    return;
                }
                String filename = args.length >= 3 ? args[2] : "redbug.vcd";
                if (!filename.endsWith(".vcd")) filename += ".vcd";
                try {
                    Path path = probeManager.startVcd(filename);
                    String baseName = filename.replace(".vcd", "");
                    player.sendMessage(Component.text("\u26a1 VCD recording ", NamedTextColor.GREEN)
                            .append(Component.text("STARTED", NamedTextColor.AQUA)));
                    player.sendMessage(Component.text("  File: " + path, NamedTextColor.GRAY));
                    player.sendMessage(Component.text("  Live view: ", NamedTextColor.GRAY)
                            .append(Component.text("./" + baseName + "_live.sh", NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("  (uses shmidcat on Linux/WSL, Tcl reload on macOS)", NamedTextColor.DARK_GRAY));
                } catch (IOException e) {
                    player.sendMessage(Component.text("Failed to start VCD: " + e.getMessage(), NamedTextColor.RED));
                }
            }
            case "stop" -> {
                if (!probeManager.isRecordingVcd()) {
                    player.sendMessage(Component.text("Not recording.", NamedTextColor.RED));
                    return;
                }
                probeManager.stopVcd();
                player.sendMessage(Component.text("\u26a1 VCD recording ", NamedTextColor.YELLOW)
                        .append(Component.text("STOPPED", NamedTextColor.RED)));
            }
            default -> player.sendMessage(Component.text("Usage: /rb vcd <start [filename]|stop>", NamedTextColor.RED));
        }
    }

    private void handleBus(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /rb bus <scope> <sort|format|info|reset>", NamedTextColor.RED));
            return;
        }

        int scope;
        try {
            scope = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid scope number.", NamedTextColor.RED));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "sort" -> handleBusSort(player, scope, args);
            case "format" -> handleBusFormat(player, scope, args);
            case "info" -> handleBusInfo(player, scope);
            case "reset" -> handleBusReset(player, scope);
            default -> player.sendMessage(Component.text("Usage: /rb bus <scope> <sort|format|info|reset>", NamedTextColor.RED));
        }
    }

    private void handleBusSort(Player player, int scope, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Strategies: " + String.join(", ", BusConfig.SortStrategy.ids()), NamedTextColor.GRAY));
            return;
        }

        BusConfig.SortStrategy strategy = BusConfig.SortStrategy.fromId(args[3]);
        if (strategy == null) {
            player.sendMessage(Component.text("Unknown strategy. Options: " + String.join(", ", BusConfig.SortStrategy.ids()), NamedTextColor.RED));
            return;
        }

        BusConfig config = probeManager.getOrCreateBusConfig(scope);
        config.setSort(strategy);

        player.sendMessage(Component.text("\u26a1 Scope " + scope + " bus sort: ", NamedTextColor.GREEN)
                .append(Component.text(strategy.getId(), NamedTextColor.AQUA))
                .append(Component.text(" (" + strategy.getDescription() + ")", NamedTextColor.GRAY)));
    }

    private void handleBusFormat(Player player, int scope, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Formats: " + String.join(", ", BusConfig.DisplayFormat.ids()), NamedTextColor.GRAY));
            return;
        }

        BusConfig.DisplayFormat format = BusConfig.DisplayFormat.fromId(args[3]);
        if (format == null) {
            player.sendMessage(Component.text("Unknown format. Options: " + String.join(", ", BusConfig.DisplayFormat.ids()), NamedTextColor.RED));
            return;
        }

        BusConfig config = probeManager.getOrCreateBusConfig(scope);
        config.setFormat(format);

        if (args.length >= 5 && format == BusConfig.DisplayFormat.HEX) {
            try {
                int wordSize = Integer.parseInt(args[4]);
                config.setWordSize(wordSize);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid word size, using default 8.", NamedTextColor.YELLOW));
            }
        }

        String msg = format.getId();
        if (format == BusConfig.DisplayFormat.HEX) {
            msg += " (word size: " + config.getWordSize() + " bits)";
        }

        player.sendMessage(Component.text("\u26a1 Scope " + scope + " bus format: ", NamedTextColor.GREEN)
                .append(Component.text(msg, NamedTextColor.AQUA)));
    }

    private void handleBusInfo(Player player, int scope) {
        BusConfig config = probeManager.getBusConfig(scope);
        if (config == null) {
            player.sendMessage(Component.text("No bus config for scope " + scope + ". Use /rb bus " + scope + " sort <strategy>", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("\u2550\u2550\u2550 Bus Config: Scope " + scope + " \u2550\u2550\u2550", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Sort: ", NamedTextColor.GRAY)
                .append(Component.text(config.getSort().getId() + " - " + config.getSort().getDescription(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Format: ", NamedTextColor.GRAY)
                .append(Component.text(config.getFormat().getId(), NamedTextColor.WHITE)));
        if (config.getFormat() == BusConfig.DisplayFormat.HEX) {
            player.sendMessage(Component.text("  Word size: ", NamedTextColor.GRAY)
                    .append(Component.text(config.getWordSize() + " bits", NamedTextColor.WHITE)));
        }

        // Show current value if probes exist
        Map<Integer, List<Probe>> grouped = probeManager.getProbesGrouped(player.getUniqueId());
        List<Probe> probes = grouped.get(scope);
        if (probes != null && !probes.isEmpty()) {
            List<Probe> sorted = config.sortProbes(probes);
            long value = config.computeValue(sorted);
            int bits = sorted.size();
            player.sendMessage(Component.text("  Value: ", NamedTextColor.GRAY)
                    .append(Component.text(config.formatValue(value, bits), NamedTextColor.AQUA))
                    .append(Component.text(" = " + value + " = " + config.toBitsString(value, bits) + "b", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("  Bit order (LSB\u2192MSB): ", NamedTextColor.GRAY));
            for (int i = 0; i < sorted.size(); i++) {
                Probe p = sorted.get(i);
                player.sendMessage(Component.text("    bit" + i + " " + p.coordString(), WaveformRenderer.colorFor(i))
                        .append(Component.text(" = " + (p.getCurrentLevel() > 0 ? "1" : "0"), NamedTextColor.WHITE)));
            }
        }
    }

    private void handleBusReset(Player player, int scope) {
        probeManager.removeBusConfig(scope);
        player.sendMessage(Component.text("\u26a1 Bus config removed for scope " + scope + ".", NamedTextColor.YELLOW));
    }

    private void handleView(Player player) {
        Map<Integer, List<Probe>> grouped = probeManager.getProbesGrouped(player.getUniqueId());
        if (grouped.isEmpty()) {
            player.sendMessage(Component.text("No active probes. Use ", NamedTextColor.GRAY)
                    .append(Component.text("/rb probe [scope]", NamedTextColor.YELLOW)));
            return;
        }

        for (Map.Entry<Integer, List<Probe>> entry : grouped.entrySet()) {
            int scope = entry.getKey();
            player.sendMessage(Component.text("\u2550\u2550\u2550 Scope " + scope + " \u2550\u2550\u2550", NamedTextColor.GOLD));
            for (int i = 0; i < entry.getValue().size(); i++) {
                player.sendMessage(WaveformRenderer.renderCompactWaveform(entry.getValue().get(i), i));
            }

            BusConfig bus = probeManager.getBusConfig(scope);
            if (bus != null) {
                List<Probe> sorted = bus.sortProbes(entry.getValue());
                long value = bus.computeValue(sorted);
                int bits = sorted.size();
                player.sendMessage(Component.text("  Bus: ", NamedTextColor.GRAY)
                        .append(Component.text(bus.formatValue(value, bits), NamedTextColor.AQUA))
                        .append(Component.text(" (" + bus.toBitsString(value, bits) + "b)", NamedTextColor.DARK_GRAY)));
            }
        }
    }

    private void handleList(Player player) {
        Map<Integer, List<Probe>> grouped = probeManager.getProbesGrouped(player.getUniqueId());
        if (grouped.isEmpty()) {
            player.sendMessage(Component.text("No active probes.", NamedTextColor.GRAY));
            return;
        }

        int total = grouped.values().stream().mapToInt(List::size).sum();
        player.sendMessage(Component.text("Active Probes (" + total + "/" + ProbeManager.MAX_PROBES_PER_PLAYER + "):", NamedTextColor.GOLD));

        for (Map.Entry<Integer, List<Probe>> entry : grouped.entrySet()) {
            int scope = entry.getKey();
            BusConfig bus = probeManager.getBusConfig(scope);
            String busLabel = bus != null ? " [bus: " + bus.getSort().getId() + " " + bus.getFormat().getId() + "]" : "";
            player.sendMessage(Component.text("  Scope " + scope + busLabel + ":", NamedTextColor.YELLOW));
            for (int i = 0; i < entry.getValue().size(); i++) {
                Probe probe = entry.getValue().get(i);
                player.sendMessage(Component.text("    P" + (i + 1) + " " + probe.coordString(), WaveformRenderer.colorFor(i))
                        .append(Component.text(" \u2192 " + probe.getCurrentLevel(), NamedTextColor.WHITE)));
            }
        }
    }

    private void handleClear(Player player, String[] args) {
        if (args.length >= 2) {
            try {
                int scope = Integer.parseInt(args[1]);
                probeManager.clearScope(player.getUniqueId(), scope);
                player.sendMessage(Component.text("\u26a1 Scope " + scope + " cleared.", NamedTextColor.YELLOW));
                return;
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid scope number.", NamedTextColor.RED));
                return;
            }
        }
        probeManager.clearProbes(player.getUniqueId());
        player.sendMessage(Component.text("\u26a1 All probes cleared.", NamedTextColor.YELLOW));
    }

    private void showHelp(Player player) {
        String url = "http://localhost:8080";
        player.sendMessage(Component.text("\u2550\u2550\u2550 ", NamedTextColor.GOLD)
                .append(Component.text("\u26a1 Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text(" \u2550\u2550\u2550", NamedTextColor.GOLD)));
        player.sendMessage(Component.text("  /rb probe [scope]", NamedTextColor.AQUA)
                .append(Component.text(" - Toggle probe mode (default scope 1)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb bus <scope> sort <strategy>", NamedTextColor.AQUA)
                .append(Component.text(" - Set bit ordering", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb bus <scope> format <fmt> [ws]", NamedTextColor.AQUA)
                .append(Component.text(" - Set display (bin/dec/hex)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb bus <scope> info", NamedTextColor.AQUA)
                .append(Component.text(" - Show bus config and current value", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb bus <scope> reset", NamedTextColor.AQUA)
                .append(Component.text(" - Remove bus interpretation", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb vcd start [file]", NamedTextColor.AQUA)
                .append(Component.text(" - Record to VCD (for GTKWave)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb vcd stop", NamedTextColor.AQUA)
                .append(Component.text(" - Stop VCD recording", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb view", NamedTextColor.AQUA)
                .append(Component.text(" - Waveform snapshot", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb list", NamedTextColor.AQUA)
                .append(Component.text(" - List probes", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /rb clear [scope]", NamedTextColor.AQUA)
                .append(Component.text(" - Remove probes", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Web UI: ", NamedTextColor.GRAY)
                .append(Component.text(url, NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url))));
    }
}
