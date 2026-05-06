package mc.mrd_og.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.mrd_og.redbug.objects.node.NodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class RedbugCommand2 implements BasicCommand {

    private NodeManager nodeManager;

    public RedbugCommand2(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }
    }


    private void showHelp(Player player) {
        String url = "http://localhost:8080";
        player.sendMessage(Component.text("===", NamedTextColor.GOLD)
                .append(Component.text("[RB] Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text("===", NamedTextColor.GOLD)));
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
