package mc.mrd_og.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.mrd_og.redbug.objects.monitor.Monitor;
import mc.mrd_og.redbug.objects.monitor.MonitorManager;
import mc.mrd_og.redbug.objects.node.NodeManager;
import mc.mrd_og.redbug.objects.node.NodeType;
import mc.mrd_og.redbug.objects.test.TestManager;
import mc.mrd_og.redbug.util.CsvHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TestCommand implements BasicCommand {


    private static final List<String> SUBCOMMANDS = List.of("load", "remove", "clear", "run", "stop", "list", "info", "help");

    private final NodeManager nodeManager;
    private final TestManager testManager;
    private final MonitorManager monitorManager;

    public TestCommand(NodeManager nodeManager, MonitorManager monitorManager, TestManager testManager) {
        this.nodeManager = nodeManager;
        this.monitorManager = monitorManager;
        this.testManager = testManager;
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
            case "load" -> handleLoad(player, args);
            case "remove" -> handleRemove(player, args);
            case "clear" -> handleClear(player, args);
            case "run" -> handleRun(player, args);
            case "stop" -> handleStop(player, args);
            case "list" -> handleList(player,args);
            case "info" -> handleInfo(player, args);
            case "help" -> showHelp(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use /pin help", NamedTextColor.RED));
        }
    }

    private void handleLoad(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /test help", NamedTextColor.RED));
            return;
        }

        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        String testName = args[1];
        String testCsvPath = args[2];

        // validate that csv exists

        File f = new File(testCsvPath);
        if(!f.exists() || f.isDirectory()) {
            player.sendMessage(Component.text("[RB]: CSV " + testCsvPath + " does not exist", NamedTextColor.RED));
            return;
        }

        // parse CSV data
        HashMap<String, List<String>> csvData;

        try {
            csvData = CsvHelper.extractColumns(Path.of(testCsvPath));
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Component.text("[RB]: Invalid CSV Format is empty or has mismatch row lengths", NamedTextColor.RED));
            return;
        }

        if (!testManager.addTest(player, testName, csvData)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " already exists", NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("[RB]: Test " + testName + " loaded successfully", NamedTextColor.GOLD));
        }

    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /test help", NamedTextColor.RED));
            return;
        }

        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        String testName = args[1];

        if (testManager.removeTest(player, testName)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " removed", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("[RB]: Test " + testName + " does not exist", NamedTextColor.RED));
        }
    }

    private void handleClear(Player player, String[] args) {

        if (args.length > 1) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        testManager.deleteAllTests(player);
        player.sendMessage(Component.text("[RB]: All tests deleted", NamedTextColor.GOLD));

    }

    private void handleRun(Player player, String[] args) {

        if (args.length < 5) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /test help", NamedTextColor.RED));
            return;
        }

        if (args.length > 5) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        String testName = args[1];
        String monitorName = args[2];
        String clkPinName = args[3];

        int clkPeriod = -1;
        try {
            clkPeriod = 2 * Integer.parseInt(args[4]);
            if (clkPeriod < 0) {
                player.sendMessage(Component.text("[RB]: Expected positive integer clock period", NamedTextColor.RED));
                return;
            }
        } catch (Exception ignored) {
            player.sendMessage(Component.text("[RB]: Expected positive integer clock period", NamedTextColor.RED));
            return;
        }

        if (!testManager.playerHasTest(player, testName)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " does not exist", NamedTextColor.RED));
            return;
        }

        if (monitorManager.doesntHaveMonitor(player, monitorName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " does not exist", NamedTextColor.RED));
            return;
        }

        if (!nodeManager.playerHasNode(player, clkPinName)) {
            player.sendMessage(Component.text("[RB]: Pin " + clkPinName + " does not exist", NamedTextColor.RED));
            return;
        }

        if (nodeManager.getNodeType(player, clkPinName) != NodeType.PIN) {
            player.sendMessage(Component.text("[RB]: Node " + clkPinName + " must be a PIN", NamedTextColor.RED));
            return;
        }

        if(testManager.startTest(player, testName, monitorName, clkPinName, clkPeriod))
            player.sendMessage(Component.text("[RB]: Test " + testName + " started", NamedTextColor.GOLD));

    }

    private void handleStop(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /test help", NamedTextColor.RED));
            return;
        }

        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        String testName = args[1];

        if (testManager.stopTest(player, testName)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " stopped", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("[RB]: Test " + testName + " does not exist", NamedTextColor.RED));
        }

    }

    private void handleList(Player player, String[] args) {

        Set<String> playerTests = testManager.getPlayerTests(player);

        if (playerTests.isEmpty()) {
            player.sendMessage(Component.text("[RB]: No tests present try /test load <test_name> <test.csv>", NamedTextColor.GOLD));
            return;
        }

        TextComponent message = Component.text("[RB]: Found " + playerTests.size() + " test(s): ", NamedTextColor.GOLD);

        for (String name : playerTests) {
            message = message.appendNewline().append(Component.text(" -> " + name, NamedTextColor.AQUA));
        }

        player.sendMessage(message);

    }

    private void handleInfo(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few parameters try /test help", NamedTextColor.RED));
            return;
        }

        if (args.length > 3) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /test help", NamedTextColor.RED));
            return;
        }

        String testName = args[1];

        if (!testManager.printTestInfo(player, testName)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + "does not exist try /test load <test.csv> <test_name>", NamedTextColor.RED));
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

        switch (sub) {
            case "load", "remove", "run", "stop", "info" -> {

                if (args.length > 2) {
                    break;
                }

                Set<String> tests = testManager.getPlayerTests(player);
                if (tests.isEmpty())
                    tests.add("name");

                return tests.stream().filter(s -> s.startsWith(args[1])).toList();
            }
        }

        if (sub.equals("load") && args.length == 3) {
            return Stream.of("trace.csv").filter(s -> s.startsWith(args[1])).toList();
        }

        if (sub.equals("test")) {

            if (args.length == 3)  {
                // suggest monitor names

                HashMap<String, Monitor> monitors =  monitorManager.getMonitors(player);

                if (monitors == null)
                    return List.of("monitor_name");

                Set<String> monitorNames = monitors.keySet();

                if (monitorNames.isEmpty())
                    return List.of("monitor_name");

                return monitorNames.stream().filter(s -> s.startsWith(args[2])).toList();
            }
            if (args.length == 4) {
                // suggest pin names
                List<String> nodes = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());
                nodes = nodes.stream().filter(s -> nodeManager.getNodeType(player, s) == NodeType.PIN).toList();
                nodes = nodes.stream().filter(s -> s.startsWith(args[3])).toList();

                if (nodes.isEmpty()) {
                    nodes.add("clock_pin_name");
                }

                return nodes;
            }
            if (args.length == 5 || args.length == 6) {
                return List.of("10", "20", "30", "40");
            }
        }

        return List.of();
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("===", NamedTextColor.GOLD)
                .append(Component.text("[RB]: Redbug - Redstone Oscilloscope", NamedTextColor.RED))
                .append(Component.text("===", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("  /test load <test_name> <csv_path.csv>", NamedTextColor.AQUA)
                .append(Component.text(" - Load a csv and save to a test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test remove <monitor_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove a test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test clear", NamedTextColor.AQUA)
                .append(Component.text(" - Delete all tests", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test run <test_name> <monitor_name> <clk_pin_name> <clock_period in ticks> <vcd sample delay in ticks>", NamedTextColor.AQUA)
                .append(Component.text(" - Begin a test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test stop <test_name>", NamedTextColor.AQUA)
                .append(Component.text(" - Stop a test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test list", NamedTextColor.AQUA)
                .append(Component.text(" - List all tests", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /test list <test_name>", NamedTextColor.AQUA)
                .append(Component.text(" - List information about specified test", NamedTextColor.GRAY)));

    }
}
