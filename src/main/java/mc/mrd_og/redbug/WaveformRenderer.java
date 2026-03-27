package mc.mrd_og.redbug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class WaveformRenderer {

    private static final char[] BARS = {' ', '\u2581', '\u2582', '\u2583', '\u2584', '\u2585', '\u2586', '\u2587', '\u2588'};

    public static final NamedTextColor[] CHANNEL_COLORS = {
            NamedTextColor.RED,
            NamedTextColor.BLUE,
            NamedTextColor.GREEN,
            NamedTextColor.YELLOW,
            NamedTextColor.AQUA,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.GOLD,
            NamedTextColor.WHITE
    };

    private WaveformRenderer() {
    }

    public static NamedTextColor colorFor(int channelIndex) {
        return CHANNEL_COLORS[channelIndex % CHANNEL_COLORS.length];
    }

    public static char signalToChar(int level) {
        if (level < 0) return '?';
        int index = Math.round(level * 8f / 15f);
        return BARS[Math.min(index, BARS.length - 1)];
    }

    public static Component renderCompactWaveform(Probe probe, int channelIndex) {
        NamedTextColor color = colorFor(channelIndex);
        LinkedList<Integer> history = probe.getHistory();
        StringBuilder waveform = new StringBuilder();
        for (int level : history) {
            waveform.append(signalToChar(level));
        }

        return Component.text("CH" + (channelIndex + 1) + " ", NamedTextColor.GRAY)
                .append(Component.text("[" + probe.getCurrentLevel() + "] ", color))
                .append(Component.text(waveform.toString(), color));
    }

    public static Component renderActionBar(List<Probe> probes) {
        if (probes.isEmpty()) {
            return Component.text("No probes active", NamedTextColor.GRAY);
        }

        Component result = Component.empty();
        for (int i = 0; i < probes.size(); i++) {
            Probe probe = probes.get(i);
            NamedTextColor color = colorFor(i);
            LinkedList<Integer> history = probe.getHistory();

            StringBuilder mini = new StringBuilder();
            int start = Math.max(0, history.size() - 12);
            for (int j = start; j < history.size(); j++) {
                mini.append(signalToChar(history.get(j)));
            }

            Component channel = Component.text("CH" + (i + 1) + ":", color)
                    .append(Component.text(mini.toString(), color));

            if (i > 0) {
                result = result.append(Component.text(" \u2502 ", NamedTextColor.DARK_GRAY));
            }
            result = result.append(channel);
        }
        return result;
    }

    public static List<Component> renderFullDisplay(List<Probe> probes) {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.text("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550 ", NamedTextColor.GOLD)
                .append(Component.text("\u26a1 Redbug Oscilloscope", NamedTextColor.RED))
                .append(Component.text(" \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550", NamedTextColor.GOLD)));

        if (probes.isEmpty()) {
            lines.add(Component.text("  No probes active. Use ", NamedTextColor.GRAY)
                    .append(Component.text("/rb probe", NamedTextColor.YELLOW))
                    .append(Component.text(" to add probes.", NamedTextColor.GRAY)));
        } else {
            for (int i = 0; i < probes.size(); i++) {
                Probe probe = probes.get(i);
                NamedTextColor color = colorFor(i);
                lines.add(Component.text(" CH" + (i + 1) + " " + probe.coordString() + ": ", color)
                        .append(Component.text("Signal=" + probe.getCurrentLevel(), NamedTextColor.WHITE)));
                lines.add(Component.text("  ").append(renderCompactWaveform(probe, i)));
            }
        }

        lines.add(Component.text("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550", NamedTextColor.GOLD));

        return lines;
    }
}
