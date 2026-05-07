package mc.md5g22.redbug.objects.monitor;

import mc.md5g22.redbug.plugin.Redbug;
import mc.md5g22.redbug.objects.node.Node;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.vcd.VcdManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MonitorManager {

    private static MonitorManager monitorManager;
    private NodeManager nodeManager;
    private VcdManager vcdManager;


    private final HashMap<UUID, HashMap<String, Monitor>> playerMonitors = new HashMap<>();


    private BukkitTask sampleTask;
    private final Redbug plugin;

    public static MonitorManager getInstance(Redbug plugin) {
        if (monitorManager == null) {
            monitorManager = new MonitorManager(plugin);
        }
        return monitorManager;
    }

    public MonitorManager(Redbug plugin) {
        this.plugin = plugin;
    }

    public void setNodeManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void setVcdManager(VcdManager vcdManager) {
        this.vcdManager = vcdManager;
    }

    public void start() {
        sampleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (HashMap<String, Node> nodeMap : nodeManager.getPlayerNodes().values()) {
                    for (Node n : nodeMap.values()) {
                        n.sample();
                    }
                }

                for (Map.Entry<UUID, HashMap<String, Monitor>> entry: playerMonitors.entrySet()) {
                    HashMap<String, Monitor> monitors = entry.getValue();
                    UUID uuid = entry.getKey();

                    for (Monitor monitor : monitors.values()) {
                        try {
                            vcdManager.writeSample(plugin.getServer().getPlayer(uuid), monitor);
                        } catch (IOException e) {
                            vcdManager.stopDump(plugin.getServer().getPlayer(entry.getKey()), monitor);
                            throw new RuntimeException(e);
                        }
                    }
                }

            }

        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void stop() {
        if (sampleTask != null) sampleTask.cancel();
    }

    public HashMap<String, Monitor> getMonitors(Player player) {
        if (playerMonitors.containsKey(player.getUniqueId())) {
            return playerMonitors.get(player.getUniqueId());
        }
        return null;
    }

    public boolean doesntHaveMonitor(Player player, String monitorName) {
        if (monitorName == null)
            return true;

        return !getMonitors(player).containsKey(monitorName);
    }

    public void clearMonitors(Player player) {
        UUID uuid = player.getUniqueId();

        if (playerMonitors.containsKey(uuid)) {
            playerMonitors.get(uuid).clear();
        }

        player.sendMessage(Component.text("[RB]: Monitors cleared.", NamedTextColor.GOLD));
    }

    public void createMonitor(Player player, String monitorName) {
        HashMap<String, Monitor> monitors = getMonitors(player);

        UUID uuid = player.getUniqueId();

        if (monitors == null) {
            monitors = new HashMap<>();
            playerMonitors.put(uuid, monitors);
        }

        // Treat as a set
        if (monitors.containsKey(monitorName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " already exists.", NamedTextColor.RED));
            return;
        }

        monitors.put(monitorName, new Monitor(monitorName));
        player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " created.", NamedTextColor.GOLD));
    }

    public void deleteMonitor(Player player, String monitorName) {
        HashMap<String, Monitor> monitors = getMonitors(player);

        UUID uuid = player.getUniqueId();

        if (monitors == null) {
            monitors = new HashMap<>();
            playerMonitors.put(uuid, monitors);
        }

        if (doesntHaveMonitor(player, monitorName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " does not exist.", NamedTextColor.RED));
            return;
        }

        monitors.remove(monitorName);
        player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " deleted.", NamedTextColor.GOLD));
    }

    public void addNodeToMonitor(Player player, String monitorName, String nodeName) {
        HashMap<String, Monitor> monitors = getMonitors(player);

        if (doesntHaveMonitor(player, monitorName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " does not exist.", NamedTextColor.RED));
            return;
        }

        NodeManager n = NodeManager.getInstance(plugin);

        if (!n.playerHasNode(player, nodeName)){
            player.sendMessage(Component.text("[RB]: Node " + monitorName + " does not exist.", NamedTextColor.RED));
            return;
        }

        if (monitors.get(monitorName).hasMonitorSource(nodeName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " already monitors " + nodeName, NamedTextColor.RED));
        }

        monitors.get(monitorName).addMonitorSource(nodeName);
        player.sendMessage(Component.text("[RB]: Node " + nodeName + " added to " + monitorName, NamedTextColor.GOLD));
    }

    public void removeNodeFromMonitor(Player player, String monitorName, String nodeName) {
        HashMap<String, Monitor> monitors = getMonitors(player);

        if (doesntHaveMonitor(player, monitorName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " does not exist.", NamedTextColor.RED));
            return;
        }

        NodeManager n = NodeManager.getInstance(plugin);

        if (!n.playerHasNode(player, nodeName)){
            player.sendMessage(Component.text("[RB]: Node " + monitorName + " does not exist.", NamedTextColor.RED));
            return;
        }

        if (!monitors.get(monitorName).hasMonitorSource(nodeName)) {
            player.sendMessage(Component.text("[RB]: Monitor " + monitorName + " does not monitor " + nodeName, NamedTextColor.RED));
        }

        monitors.get(monitorName).removeMonitorSource(nodeName);
        player.sendMessage(Component.text("[RB]: Node " + nodeName + " removed from " + monitorName, NamedTextColor.GOLD));
    }

    public HashMap<UUID, HashMap<String, Monitor>> getAllPlayerMonitors() {
        return playerMonitors;
    }

}
