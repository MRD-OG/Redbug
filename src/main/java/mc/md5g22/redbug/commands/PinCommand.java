package mc.md5g22.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.objects.node.NodeType;
import mc.md5g22.redbug.util.BinaryHelper;
import mc.md5g22.redbug.util.ColourHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PinCommand extends NodeCommand implements BasicCommand {


    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "toggle", "set", "pulse", "clear", "list", "help");

    public PinCommand(NodeManager nodeManager) {
        super(nodeManager);
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

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(player, args, NodeType.PIN);
            case "remove" -> handleRemove(player, args, NodeType.PIN);
            case "toggle" -> handleToggle(player, args);
            case "set" -> handleSet(player, args);
            case "pulse" -> handlePulse(player, args);
            case "clear" -> handleClear(player, args, NodeType.PIN);
            case "list" -> handleList(player, args, NodeType.PIN);
            case "help" -> showHelp(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use /pin help", NamedTextColor.RED));
        }
    }

    private void handleToggle(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few arguments try /pin help", NamedTextColor.RED));
            return;
        }

        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
            return;
        }

        String nodeName = args[1];

        nodeManager.pinOperation(player, nodeName, null, true, -1);

    }

    private void handleSet(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("[RB]: Too few arguments try /pin help", NamedTextColor.RED));
            return;
        }

        String nodeName = args[1];
        Integer value = null;

        try {
            value = Integer.parseInt(args[2]);
        } catch (Exception ignored) {
            player.sendMessage(Component.text("[RB]: Value must be an integer", NamedTextColor.RED));
            return;
        }

        String binary_value = null;

        int bits = nodeManager.getNodeSize(player.getUniqueId(), nodeName);
        System.out.println(nodeName + " bit length: " + bits);

        if (bits < 0) {
            player.sendMessage(Component.text("[RB]: Pin " + nodeName + " does not exist try /pin add", NamedTextColor.RED));
            return;
        }

        if (value < 0) {
            binary_value = BinaryHelper.toSignedBinary(value, bits);
        } else {
            binary_value = BinaryHelper.toUnsignedBinary(value, bits);
        }

        if (binary_value == null) {
            player.sendMessage(Component.text("[RB]: Value " + value + " outside of pin [" + nodeName + "] bit range", NamedTextColor.RED));
            return;
        }

        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
            return;
        }

        nodeManager.pinOperation(player, nodeName, binary_value, false, -1);
    }

    private void handlePulse(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("[RB]: Too few arguments try /pin help", NamedTextColor.RED));
            return;
        }

        String nodeName = args[1];
        Integer value = null;

        try {
            value = Integer.parseInt(args[2]);
        } catch (Exception ignored) {
            player.sendMessage(Component.text("[RB]: Value must be an integer", NamedTextColor.RED));
            return;
        }

        String binary_value = null;

        int bits = nodeManager.getNodeSize(player.getUniqueId(), nodeName);

        if (bits < 0) {
            player.sendMessage(Component.text("[RB]: Pin " + nodeName + " does not exist try /pin add", NamedTextColor.RED));
            return;
        }

        if (value < 0) {
            binary_value = BinaryHelper.toSignedBinary(value, bits);
        } else {
            binary_value = BinaryHelper.toUnsignedBinary(value, bits);
        }

        if (binary_value == null) {
            player.sendMessage(Component.text("[RB]: Value " + value + " outside of pin [" + nodeName + "] bit range", NamedTextColor.RED));
            return;
        }

        int duration = -1;

        try {
            duration = 2 * Integer.parseInt(args[3]);
            if (duration < 0) {
                player.sendMessage(Component.text("[RB]: Duration must be a positive integer", NamedTextColor.RED));
                return;
            }
        } catch (Exception ignored) {
            player.sendMessage(Component.text("[RB]: Duration must be a positive integer", NamedTextColor.RED));
            return;
        }

        if (args.length > 4) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
            return;
        }

        nodeManager.pinOperation(player, nodeName, binary_value, false, duration);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {

        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return List.of("");
        }

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }

        String sub = args[0].toLowerCase();

        if ((sub.equals("add") || sub.equals("remove")) && args.length == 2) {
            return nodeManager.getPlayerNodeNames(player.getUniqueId()).stream()
                    .filter(s -> s.startsWith(args[1])).toList();
        }

        if (sub.equals("add")) {
            if (args.length == 3) {
                return List.of("1", "2", "3", "4", "5", "6", "7", "8").stream()
                        .filter(s -> s.startsWith(args[2])).toList();
            }

            if (args.length == 4) {
                return ColourHelper.COLOURS.stream()
                        .filter(s -> s.startsWith(args[3])).toList();
            }
        }

        // todo: add toggle suggestion
        // todo: add pin set suggestions
        // todo: add pulse suggestions
        // todo: add toggle suggestions

        if (sub.equals("list") && args.length == 2) {
            List<String> nodeNames = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());

            if (!nodeManager.getPlayerNodes().containsKey(player.getUniqueId())) {
                return List.of("name");
            }

            var playerNodes = nodeManager.getPlayerNodes().get(player.getUniqueId());

            ArrayList<String> pinNodes = new ArrayList<>();

            for (String key : playerNodes.keySet()) {
                if (playerNodes.get(key).getType() == NodeType.PIN) {
                    pinNodes.add(key);
                }
            }

            if (pinNodes.isEmpty()) {
                pinNodes.add("name");
            }

            return pinNodes.stream().filter(s -> s.startsWith(args[1])).toList();
        }

        return List.of();
    }

    private void showHelp(Player player) {
        String url = "http://localhost:8080";
        player.sendMessage(Component.text("\u2550\u2550\u2550", NamedTextColor.GOLD)
                .append(Component.text("[RB]: Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text("\u2550\u2550\u2550", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("  /pin add <name> <size> <colour>", NamedTextColor.AQUA)
                .append(Component.text(" - Toggle pin add mode", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /pin remove <name>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove a pin", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /pin toggle <name>", NamedTextColor.AQUA)
                .append(Component.text(" - Flip all bits in pin", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /pin set <name> <value>", NamedTextColor.AQUA)
                .append(Component.text(" - Set pin value", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /pin pulse <name> <value> <duration>", NamedTextColor.AQUA)
                .append(Component.text(" - Pulse pin with <value> for <duration> redstone ticks", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /pin clear", NamedTextColor.AQUA)
                .append(Component.text(" - Remove all pins", NamedTextColor.GRAY)));

        player.sendMessage(Component.text("  Web UI: ", NamedTextColor.GRAY)
                .append(Component.text(url, NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url))));
    }
}
