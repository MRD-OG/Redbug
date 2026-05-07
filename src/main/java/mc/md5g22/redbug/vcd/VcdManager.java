package mc.md5g22.redbug.vcd;

import mc.md5g22.redbug.objects.monitor.MonitorManager;
import mc.md5g22.redbug.objects.node.Node;
import mc.md5g22.redbug.plugin.Redbug;
import mc.md5g22.redbug.objects.monitor.Monitor;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.objects.test.Test;
import mc.md5g22.redbug.util.VcdIdGenerator;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VcdManager {

    private static VcdManager vcdManager;
    private NodeManager nodeManager;
    private MonitorManager monitorManager;

    private Redbug plugin;

    private HashMap<Test, Monitor> testMonitors = new HashMap<>();
    private HashMap<Monitor, BufferedWriter> monitorVcdWriters = new HashMap<>();
    private HashMap<Monitor, Integer> clkOffsets = new HashMap<>();
    private HashMap<Monitor, String> testClkNames = new HashMap<>();
    private HashSet<Monitor> monitorIsTest = new HashSet<>();
    //private HashMap<Monitor, LinkedList<Integer>> queuedClockSignals = new HashMap<>();
    //private HashMap<Monitor, LinkedList<Integer>> queuedClockTimestamps = new HashMap<>();
    private HashMap<Monitor, HashMap<Node, ArrayList<String>>> nodeWireIds = new HashMap<>();
    private HashMap<Monitor, HashMap<Node, String>> nodeBusIds = new HashMap<>();
    private HashMap<Monitor, HashMap<Node, ArrayList<Integer>>> lastWireValues = new HashMap<>();
    private HashMap<Monitor, HashMap<Node, String>> lastBusValues = new HashMap<>();

    private HashMap<Monitor, Integer> monitorTimestamps;

    public static VcdManager getInstance(Redbug plugin) {

        if (vcdManager == null) {
            vcdManager = new VcdManager(plugin);
        }

        return vcdManager;
    }

    private VcdManager(Redbug plugin) {
        this.plugin = plugin;
    }

    public void setNodeManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void setMonitorManager(MonitorManager monitorManager) {
        this.monitorManager = monitorManager;
    }

    public void startDump(Player player, Object obj) {

        String fileName;

        String fileTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));

        Test test;
        Monitor monitor;

        if (obj instanceof Test) {
            test = (Test) obj;
            fileName = "test_" + test.getName() + "_" + fileTime + ".vcd";

            if (test.isTestRunning()) {
                return;
            }
            monitor=test.getMonitor(player);
            //clkOffsets.put(monitor, test.getClkOffset());
            testClkNames.put(monitor, test.getClkPinName());
            monitorIsTest.add(monitor);
            //queuedClockSignals.put(monitor, new LinkedList<>());
            //queuedClockTimestamps.put(monitor, new LinkedList<>());

        } else if (obj instanceof Monitor) {
            monitor = (Monitor) obj;
            fileName = "monitor_" + monitor.getName() + "_" + fileTime + ".vcd";
        } else {
            return;
        }

        BufferedWriter writer;

        Path path = Path.of(fileName);
        try {
            writer = new BufferedWriter(new FileWriter(path.toFile()));
        } catch (IOException e) {
            System.out.println(fileName);
            throw new RuntimeException(e);
        }

        monitorVcdWriters.put(monitor, writer);

        try {
            writeHeader(player, monitor, writer);
            writeInitialDump(player, monitor, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Path absPath = path.toAbsolutePath();

        String strippedFileName = fileName.replace(".vcd", "");
        try {
            writeTclScript(absPath.resolveSibling(strippedFileName + "_reload.tcl"));
            writeLaunchScript(absPath.resolveSibling(strippedFileName + "_live.sh"), fileName, strippedFileName + "_reload.tcl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean stopDump(Player player, Object obj) {



        if (monitorVcdWriters == null) {
            monitorVcdWriters = new HashMap<>();
        }

        if (obj instanceof Test test) {

            Monitor monitor = test.getMonitor(player);

            //LinkedList<Integer> timestamps = queuedClockTimestamps.get(monitor);
            //LinkedList<Integer> values = queuedClockSignals.get(monitor);

            /*
            String clkNodeName = test.getClkPinName();

            Node clk = nodeManager.getNode(player, clkNodeName);

            BufferedWriter writer = monitorVcdWriters.get(monitor);

            if (!timestamps.isEmpty() && !values.isEmpty()) {
                String clkid = nodeWireIds.get(monitor).get(clk).getFirst();

                for (int i = 0; i < timestamps.size(); i++) {
                    String timestamp = "#" + timestamps.remove().toString() + "\n";
                    String value = clk.getCurrentLevels().getFirst() + clkid + "\n";

                    try {
                        writer.write(timestamp + value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }*/

            monitorIsTest.remove(test.getMonitor(player));
            closeWriter(monitorVcdWriters.remove(test.getMonitor(player)));
            return true;
        }

        else if (obj instanceof Monitor monitor) {
            closeWriter(monitorVcdWriters.remove(monitor));
            return true;
        }

        return false;
    }

    public synchronized void closeWriter(BufferedWriter writer) {
        try {
            writer.flush();
        } catch (Exception ignored) { }
        try {
            writer.close();
        } catch (Exception ignored) { }
    }

    public void stop() {
        // Close all writer streams
        if (monitorVcdWriters != null) {
            for (BufferedWriter writer : monitorVcdWriters.values()) {
                closeWriter(writer);
            }
        }
    }

    private void writeHeader(Player player, Monitor monitor, BufferedWriter writer) throws IOException {
        writer.write("$version Redbug Oscilloscope $end\n");
        writer.write("$date " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " $end\n");
        writer.write("$timescale 100ms $end\n");
        writer.write("$scope module redbug $end\n");

        ArrayList<String> nodeNames = monitor.getMonitorSources();

        VcdIdGenerator vcdIdGenerator = new VcdIdGenerator();

        nodeBusIds = new HashMap<>();
        nodeWireIds = new HashMap<>();

        nodeBusIds.put(monitor, new HashMap<>());
        nodeWireIds.put(monitor, new HashMap<>());

        for (String nodeName : nodeNames) {


            Node node = nodeManager.getNode(player, nodeName);
            writer.write("$scope module " + nodeName + " $end\n");


            if (!nodeWireIds.get(monitor).containsKey(node)) {
                nodeWireIds.get(monitor).put(node, new ArrayList<>());
            }

            if (!nodeBusIds.get(monitor).containsKey(node)) {
                nodeBusIds.get(monitor).put(node, null);
            }

            ArrayList<Location> locations = node.getLocations();
            int bits = locations.size();

            if (bits == 1) {
                String wireName = nodeName + "_bit_0";
                String id = vcdIdGenerator.next();
                nodeWireIds.get(monitor).get(node).add(id);
                writer.write("$var wire 1 " + id + " " + wireName + " $end\n");
            } else {
                // add wires for each bit
                for (int i = 0; i < locations.size(); i++) {
                    String wireName = nodeName + "_bit_" + (bits - 1 - i);
                    String id = vcdIdGenerator.next();
                    nodeWireIds.get(monitor).get(node).add(id);
                    writer.write("$var wire 1 " + id + " " + wireName + " $end\n");
                }

                String id = vcdIdGenerator.next();
                nodeBusIds.get(monitor).put(node, id);
                writer.write("$var reg " + bits + " " + id + " bus[" + (bits - 1) + ":0] $end\n");
            }
            writer.write("$upscope $end\n");
        }
        writer.write("$enddefinitions $end\n");
        writer.flush();
    }

    private void writeInitialDump(Player player, Monitor monitor, BufferedWriter writer) throws IOException {
        writer.write("#0\n");
        writer.write("$dumpvars\n");

        ArrayList<String> nodeNames = monitor.getMonitorSources();

        lastWireValues = new HashMap<>();
        lastWireValues.put(monitor, new HashMap<>());
        lastBusValues = new HashMap<>();
        lastBusValues.put(monitor, new HashMap<>());

        HashMap<Node, ArrayList<String>> wireIds = nodeWireIds.get(monitor);
        HashMap<Node, String> busIds = nodeBusIds.get(monitor);

        for (String nodeName : nodeNames) {
            Node node = nodeManager.getNode(player, nodeName);

            ArrayList<Integer> values = node.getCurrentLevels();

            ArrayList<String> wire_ids = wireIds.get(node);

            for (int i = 0; i < values.size(); i++) {
                String wireId = wire_ids.get(i);
                int value = values.get(i);
                writer.write(value + wireId + "\n");
                lastWireValues.get(monitor).put(node, values);
            }

            if (node.getSize() > 1) {
                String busId = busIds.get(node);
                String binStr = computeBusBinary(values);
                writer.write("b" + binStr + " " + busId + "\n");
                lastBusValues.get(monitor).put(node, binStr);
            }
        }

        writer.write("$end\n");
        writer.flush();
    }

    public void writeSample(Player player, Monitor monitor) throws IOException {

        if (monitorVcdWriters == null || !monitorVcdWriters.containsKey(monitor)) {
            // Does not exist
            return;
        }

        BufferedWriter writer = monitorVcdWriters.get(monitor);

        if (monitorTimestamps == null) {
            monitorTimestamps = new HashMap<>();
        }

        if (!monitorTimestamps.containsKey(monitor)) {
            monitorTimestamps.put(monitor, 0);
        }

        int timestamp = monitorTimestamps.get(monitor);
        monitorTimestamps.put(monitor, timestamp + 1);

        StringBuilder changes = new StringBuilder();
        boolean anyChange = false;


        ArrayList<String> nodeNames = monitor.getMonitorSources();

        HashMap<Node, String> lastBuses = lastBusValues.get(monitor);
        HashMap<Node, ArrayList<Integer>> lastWires = lastWireValues.get(monitor);

        var wireIds = nodeWireIds.get(monitor);
        var busIds = nodeBusIds.get(monitor);

        String clkId = null;
        int clkOffset = -1;

        if (monitorIsTest.contains(monitor)) {
            Node clk = nodeManager.getNode(player, testClkNames.get(monitor));
            clkId = wireIds.get(clk).getFirst();
            clkOffset = clkOffsets.get(monitor);
        }

        for (String nodeName : nodeNames) {

            Node node = nodeManager.getNode(player, nodeName);

            ArrayList<Integer> currentValues = node.getCurrentLevels();


            String currentId = wireIds.get(node).getFirst();
            /*
            if (clkId != null) {

                // Is the node the clock?
                if (currentId.equals(clkId)) {
                    int cv = node.getCurrentLevels().getFirst();

                    if (lastWires.containsKey(node)) {
                        int pv = lastWires.get(node).getFirst();


                        // current != getLast?
                        if (cv != pv) {
                            // Schedule it for timestamp + offset
                            queuedClockSignals.get(monitor).add(cv);
                            queuedClockTimestamps.get(monitor).add(timestamp + clkOffset);
                        }

                    }

                    // Is there a queued timestamp?
                    if (!queuedClockTimestamps.get(monitor).isEmpty()) {
                        // Is that timestamp for now?
                        if (queuedClockTimestamps.get(monitor).getFirst() == timestamp) {

                            // Update it
                            queuedClockTimestamps.get(monitor).remove();
                            int value = queuedClockSignals.get(monitor).remove();
                            changes.append(value).append(clkId).append('\n');
                        }
                    }
                    continue;
                }
            }*/

            if (lastWires.containsKey(node)) {
                ArrayList<String> ids = wireIds.get(node);
                ArrayList<Integer> lastValues = lastWires.get(node);
                for (int i = 0; i < ids.size(); i++) {

                    String id = ids.get(i);
                    int current = currentValues.get(i);

                    if (current != lastValues.get(i)) {
                        changes.append(current).append(id).append('\n');
                        lastValues.set(i, current);
                        anyChange = true;
                    }
                }
            }

            if (lastBuses.containsKey(node)) {
                String id = busIds.get(node);
                String lastValue = lastBuses.get(node);
                String currentValue = computeBusBinary(currentValues);
                if (!currentValue.equals(lastValue)) {
                    changes.append('b').append(currentValue).append(' ').append(id).append('\n');
                    lastBuses.put(node, currentValue);
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

    private String computeBusBinary(ArrayList<Integer> values) {

        StringBuilder binString = new StringBuilder();

        for (Integer value : values) {
            binString.append(value);
        }

        return binString.toString();
    }

    private void writeTclScript(Path tclPath) throws IOException {
        try (var tw = new FileWriter(tclPath.toFile())) {
            tw.write("proc reload_live {} {\n");
            tw.write("    gtkwave::reLoadFile\n");
            tw.write("    after 1500 reload_live\n");
            tw.write("}\n");
            tw.write("after 1500 reload_live\n");
        }
    }

    private void writeLaunchScript(Path scriptPath, String vcdFile, String tclFile) throws IOException {
        try (var sw = new FileWriter(scriptPath.toFile())) {
            sw.write("#!/bin/bash\n");
            sw.write("# Redbug GTKWave live viewer\n");
            sw.write("# Usage: ./redbug_live.sh [--shm]\n");
            sw.write("#   --shm  Force shmidcat mode (native Linux only, breaks on WSL)\n\n");
            sw.write("DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n");
            sw.write("VCD=\"$DIR/" + vcdFile + "\"\n");
            sw.write("TCL=\"$DIR/" + tclFile + "\"\n\n");
            sw.write("if [ \"$1\" = \"--shm\" ]; then\n");
            sw.write("    if ! command -v shmidcat &>/dev/null; then\n");
            sw.write("        echo \"[RB]: shmidcat not found. Install gtkwave package.\"\n");
            sw.write("        exit 1\n");
            sw.write("    fi\n");
            sw.write("    echo \"[RB]: Using shmidcat live mode...\"\n");
            sw.write("    tail -c +0 -f \"$VCD\" | shmidcat | gtkwave -v -I\n");
            sw.write("else\n");
            sw.write("    echo \"[RB]: Opening GTKWave with auto-reload (every 1.5s)...\"\n");
            sw.write("    echo \"[RB]: Tip: on native Linux, try --shm for true live streaming.\"\n");
            sw.write("    gtkwave \"$VCD\" -S \"$TCL\"\n");
            sw.write("fi\n");
        }
        scriptPath.toFile().setExecutable(true);
    }
}
