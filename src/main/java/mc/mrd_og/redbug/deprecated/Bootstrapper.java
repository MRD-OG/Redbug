package mc.mrd_og.redbug.deprecated;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import mc.mrd_og.redbug.plugin.Redbug;
import org.bukkit.plugin.java.JavaPlugin;

public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {

    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new Redbug();
    }

}