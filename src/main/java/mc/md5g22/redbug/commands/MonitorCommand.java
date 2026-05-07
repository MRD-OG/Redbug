package mc.md5g22.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.md5g22.redbug.objects.monitor.Monitor;
import mc.md5g22.redbug.objects.monitor.MonitorManager;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.vcd.VcdManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

public class MonitorCommand implements BasicCommand {


    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "clear", "add", "remove", "list", "help", "vcd");
    private static final List<String> VCD_SUBCOMMANDS = List.of("start", "stop");

    private final NodeManager nodeManager;
    private final MonitorManager monitorManager;
    private final VcdManager vcdManager;

    public MonitorCommand(NodeManager nodeManager, MonitorManager monitorManager, VcdManager vcdManager) {
        this.nodeManager = nodeManager;
        this.monitorManager = monitorManager;
        this.vcdManager = vcdManager;
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
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "clear" -> handleClear(player, args);
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player,args);
            case "vcd" -> handleVcd(player ,args);
            case "help" -> showHelp(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use /pin help", NamedTextColor.RED));
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        monitorManager.createMonitor(player, args[1]);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        monitorManager.deleteMonitor(player, args[1]);
    }

    private void handleClear(Player player, String[] args) {
        if (args.length > 1) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        monitorManager.clearMonitors(player);
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        monitorManager.addNodeToMonitor(player, args[1], args[2]);
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        monitorManager.removeNodeFromMonitor(player, args[1], args[2]);
    }

    private void handleList(Player player, String[] args) {
        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        // List all monitors
        if (args.length == 1) {

            HashMap<String, Monitor> monitors = monitorManager.getMonitors(player);

            if (monitors == null) {
                player.sendMessage(Component.text("[RB]: You have no active monitors try /monitor create", NamedTextColor.GOLD));
                return;
            }

            Set<String> monitorNames = monitors.keySet();

            if (monitorNames.isEmpty()) {
                player.sendMessage(Component.text("[RB]: You have no active monitors try /monitor create", NamedTextColor.GOLD));
            } else {

                TextComponent message = Component.text("[RB]: You have " + monitorNames.size() + " active monitor(s): ", NamedTextColor.GOLD);

                for (String name : monitorNames) {
                    message = message.appendNewline().append(Component.text(" -> " + name, NamedTextColor.AQUA));
                }

                player.sendMessage(message);
            }

        } else {
            // List all nodes in monitor
            String monitorName = args[1];

            HashMap<String, Monitor> monitors = monitorManager.getMonitors(player);

            if (!monitors.containsKey(monitorName)) {
                player.sendMessage(Component.text("[RB]: Monitor [" + monitorName + "] does not exist try /monitor create", NamedTextColor.RED));
                return;
            }

            ArrayList<String> nodeNames = monitors.get(monitorName).getMonitorSources();

            if (nodeNames.isEmpty()) {
                player.sendMessage(Component.text("[RB]: You have no observed nodes try /monitor add <node_name>", NamedTextColor.GOLD));
            } else {

                TextComponent message = Component.text("[RB]: Monitor " + monitorName + " has " + nodeNames.size() + " observed node(s): ", NamedTextColor.GOLD);

                for (String name : nodeNames) {
                    message = message.appendNewline().append(Component.text(" -> " + name, NamedTextColor.AQUA));
                }

                player.sendMessage(message);
            }
        }
    }

    private void handleVcd(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /monitor help", NamedTextColor.RED));
            return;
        }

        if (args[1].equals("start")) {

            if (monitorManager.getMonitors(player) == null) {
                player.sendMessage(Component.text("[RB]: Monitor " + args[2] + " does not exist try /monitor create", NamedTextColor.RED));
                return;
            }

            if (!monitorManager.getMonitors(player).containsKey(args[2])) {
                player.sendMessage(Component.text("[RB]: Monitor " + args[2] + " does not exist try /monitor create", NamedTextColor.RED));
                return;
            }

            vcdManager.startDump(player, monitorManager.getMonitors(player).get(args[2]));
        }
        if (args[1].equals("stop")) {

            if (monitorManager.getMonitors(player) == null) {
                player.sendMessage(Component.text("[RB]: Monitor " + args[2] + " does not exist try /monitor create", NamedTextColor.RED));
                return;
            }

            if (!monitorManager.getMonitors(player).containsKey(args[2])) {
                player.sendMessage(Component.text("[RB]: Monitor " + args[2] + " does not exist try /monitor create", NamedTextColor.RED));
                return;
            }

            vcdManager.stopDump(player, monitorManager.getMonitors(player).get(args[2]));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return List.of("");
        }

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }

        String sub = args[0].toLowerCase();

        switch(sub) {
            case "create", "delete", "add", "remove", "list" -> {
                if (args.length > 2)
                    break;
                return suggestMonitors(player, args[1]);
            }
        }

        if (sub.equals("add") || sub.equals("remove") && args.length == 3) {
            List<String> nodeNames = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());

            if (nodeNames == null) {
                nodeNames = new ArrayList<>();
            }

            if (nodeNames.isEmpty()) {
                nodeNames.add("name");
            }
            return nodeNames.stream().filter(s -> s.startsWith(args[2])).toList();
        }

        if (sub.equals("vcd")) {
            if (args.length == 2) {
                return Stream.of("start", "stop").filter(s -> s.startsWith(args[1])).toList();
            }

            if (args.length == 3) {
                return suggestMonitors(player, args[2]);
            }
        }

        return List.of("");
    }

    private Collection<String> suggestMonitors(Player player, String arg) {
        HashMap<String, Monitor> monitors = monitorManager.getMonitors(player);

        if (monitors == null) {
            return List.of("name").stream().toList();
        }

        Set<String> monitorNames = monitors.keySet();

        if (monitorNames.isEmpty()) {
            monitorNames.add("name");
        }
        return monitorNames.stream().filter(s -> s.startsWith(arg)).toList();
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("===", NamedTextColor.GOLD)
                .append(Component.text("[RB]: Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text("===", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("  /monitor create <monitor_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Create a monitor", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor delete <monitor_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Delete a monitor", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor clear", NamedTextColor.AQUA)
                .append(Component.text(" - Delete all monitors", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor add <monitor_name> <node_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Add node to monitor", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor remove <monitor_name> <node_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove node from monitor", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor list", NamedTextColor.AQUA)
                .append(Component.text(" - List all monitors", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /monitor list <monitor_name>", NamedTextColor.AQUA)
                .append(Component.text(" - List all nodes observed by a monitor", NamedTextColor.GRAY)));

    }
}
