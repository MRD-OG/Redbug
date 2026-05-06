package mc.mrd_og.redbug.deprecated;

import mc.mrd_og.redbug.objects.monitor.Monitor;
import mc.mrd_og.redbug.objects.monitor.MonitorManager;
import mc.mrd_og.redbug.objects.node.Node;
import mc.mrd_og.redbug.objects.node.NodeManager;
import mc.mrd_og.redbug.plugin.Redbug;
import mc.mrd_og.redbug.deprecated.BusConfig;
import mc.mrd_og.redbug.deprecated.Probe;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class JsonBuilder {

    private Redbug plugin;
    private MonitorManager monitorManager;
    private NodeManager nodeManager;

    public JsonBuilder(Redbug plugin) {
        this.plugin = plugin;
    }

    public String buildJson() {

        this.nodeManager = NodeManager.getInstance(plugin);
        this.monitorManager = MonitorManager.getInstance(plugin);

        Set<UUID> uuids = monitorManager.getAllPlayerMonitors().keySet();

        HashSet<Player> players = new HashSet<>();

        for (UUID uuid : uuids) {
            Player player = plugin.getServer().getPlayer(uuid);
            players.add(player);
        }


        StringBuilder sb = new StringBuilder();

        // Root object
        sb.append("{\"monitors\":[");

        boolean firstPlayer = true;

        // Iterate over each player and their monitor list
        for (Player player : players) {

            // Insert comma between players
            if (!firstPlayer) {
                sb.append(',');
            }
            firstPlayer = false;

            List<String> nodeNames = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());

            HashMap<String, Monitor> monitors = monitorManager.getMonitors(player);

            // Begin player object: {"name": "<playerName>", "nodes": [...] }
            sb.append("{\"name\":\"").append(player.getName()).append("\",\"nodes\":[");

            // Emit each monitor using appendProbeJson

            boolean firstNode = true;
            for (String nodeName : nodeNames) {

                // skip if node is somehow null
                Node node = nodeManager.getNode(player, nodeName);
                if (node == null) {
                    continue;
                }

                if (!firstNode) {
                    sb.append(',');
                }
                firstNode = false;

                appendNodeJson(sb, nodeName, node);

            }

            sb.append("]}"); // End probes array + player object
        }

        sb.append("]}"); // End root object
        return sb.toString();
    }

    private void appendNodeJson(StringBuilder sb, String nodeName, Node node) {
        sb.append("{\"name\":\"").append(escapeJson(nodeName)).append("\"");
        sb.append(",\"type\":\"").append(node.getType()).append("\"");
        sb.append(",\"levels\":").append(node.getCurrentLevels());
        sb.append(",\"histories\":[");

        ArrayList<LinkedList<Integer>> histories = node.getHistories();

        boolean first = true;
        for (LinkedList<Integer> history : histories) {
            if (!first) sb.append(',');
            first = false;
            sb.append(history);
        }
        sb.append("]}");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
