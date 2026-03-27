package mc.mrd_og.redbug;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class Redbug extends JavaPlugin {

    private static final int WEB_PORT = 8080;

    private ProbeManager probeManager;
    private WebServer webServer;

    @Override
    public void onEnable() {
        probeManager = new ProbeManager(this);
        probeManager.start();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("redbug", "Redstone debug oscilloscope",
                    List.of("rb"), new RedbugCommand(probeManager));
        });

        getServer().getPluginManager().registerEvents(new ProbeListener(probeManager), this);

        webServer = new WebServer(probeManager, WEB_PORT);
        try {
            webServer.start();
            getLogger().info("Redbug Oscilloscope enabled! Web UI: http://localhost:" + WEB_PORT);
        } catch (IOException e) {
            getLogger().warning("Failed to start web server on port " + WEB_PORT + ": " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        if (probeManager != null) {
            probeManager.stop();
        }
        getLogger().info("Redbug Oscilloscope disabled.");
    }
}
