package mc.md5g22.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.md5g22.redbug.objects.node.NodeType;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.util.ColourHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WireCommand extends NodeCommand implements BasicCommand {

    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "clear", "list", "help");

    public WireCommand(NodeManager nodeManager) {
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
            case "add" -> handleAdd(player, args, NodeType.WIRE);
            case "remove" -> handleRemove(player, args, NodeType.WIRE);
            case "clear" -> handleClear(player, args, NodeType.WIRE);
            case "list" -> handleList(player, args, NodeType.WIRE);
            case "help" -> showHelp(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use /pin help", NamedTextColor.RED));
        }
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

        if (sub.equals("list") && args.length == 2) {
            List<String> nodeNames = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());

            if (!nodeManager.getPlayerNodes().containsKey(player.getUniqueId())) {
                return List.of("name");
            }

            var playerNodes = nodeManager.getPlayerNodes().get(player.getUniqueId());

            ArrayList<String> pinNodes = new ArrayList<>();

            for (String key : playerNodes.keySet()) {
                if (playerNodes.get(key).getType() == NodeType.WIRE) {
                    pinNodes.add(key);
                }
            }

            if (pinNodes.isEmpty()) {
                pinNodes.add("name");
            }

            return pinNodes.stream().filter(s -> s.startsWith(args[1])).toList();
        }


        return List.of("");
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("===", NamedTextColor.GOLD)
                .append(Component.text("[RB]: Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text("===", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("  /wire add <name> <size> <colour>", NamedTextColor.AQUA)
                .append(Component.text(" - Toggle wire add mode", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /wire remove <name>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove a wire", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /wire clear", NamedTextColor.AQUA)
                .append(Component.text(" - Remove all wires", NamedTextColor.GRAY)));

    }
}
