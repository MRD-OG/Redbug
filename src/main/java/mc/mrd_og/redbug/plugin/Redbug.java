package mc.mrd_og.redbug.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import mc.mrd_og.redbug.commands.MonitorCommand;
import mc.mrd_og.redbug.commands.PinCommand;
import mc.mrd_og.redbug.commands.TestCommand;
import mc.mrd_og.redbug.commands.WireCommand;
import mc.mrd_og.redbug.listeners.NodePlacerListener;
import mc.mrd_og.redbug.objects.monitor.MonitorManager;
import mc.mrd_og.redbug.objects.node.NodeManager;
import mc.mrd_og.redbug.objects.test.TestManager;
import mc.mrd_og.redbug.vcd.VcdManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

public final class Redbug extends JavaPlugin {

    private static final int WEB_PORT = 8080;

    private NodeManager nodeManager;
    private TestManager testManager;
    private MonitorManager monitorManager;
    private VcdManager vcdManager;
    @Override
    public void onEnable() {

        nodeManager = NodeManager.getInstance(this);
        monitorManager = MonitorManager.getInstance(this);
        testManager = TestManager.getInstance(this);
        vcdManager = VcdManager.getInstance(this);

        monitorManager.setNodeManager(nodeManager);
        monitorManager.setVcdManager(vcdManager);

        testManager.setMonitorManager(monitorManager);
        testManager.setNodeManager(nodeManager);

        vcdManager.setNodeManager(nodeManager);
        vcdManager.setMonitorManager(monitorManager);

        nodeManager.start();
        monitorManager.start();
        testManager.start();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("redbug", "Redstone debug oscilloscope",
                    List.of("pin"), new PinCommand(nodeManager));
            event.registrar().register("redbug", "Redstone debug oscilloscope",
                    List.of("wire"), new WireCommand(nodeManager));
            event.registrar().register("redbug", "Redstone debug oscilloscope",
                    List.of("monitor"), new MonitorCommand(nodeManager, monitorManager, vcdManager));
            event.registrar().register("redbug", "Redstone debug oscilloscope",
                    List.of("test"), new TestCommand(nodeManager, monitorManager, testManager));
        });

        getServer().getPluginManager().registerEvents(new NodePlacerListener(nodeManager), this);
    }

    @Override
    public void onDisable() {
        if (nodeManager != null) {
            nodeManager.stop();
        }
        if (monitorManager != null) {
            monitorManager.stop();
        }
        if (vcdManager != null) {
            vcdManager.stop();
        }
        getLogger().info("Redbug Oscilloscope disabled.");
    }
}
