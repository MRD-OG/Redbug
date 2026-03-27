package mc.mrd_og.redbug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ProbeListener implements Listener {

    private final ProbeManager probeManager;

    public ProbeListener(ProbeManager probeManager) {
        this.probeManager = probeManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!probeManager.isInProbeMode(player.getUniqueId())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        int scope = probeManager.getProbeModeScope(player.getUniqueId());
        ProbeManager.AddResult result = probeManager.addProbe(player.getUniqueId(), block.getLocation(), scope);

        switch (result) {
            case ADDED -> {
                int total = probeManager.getProbes(player.getUniqueId()).size();
                player.sendMessage(Component.text("\u26a1 Probe added to scope " + scope + " at ", NamedTextColor.GREEN)
                        .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")", NamedTextColor.WHITE))
                        .append(Component.text(" [" + total + "/" + ProbeManager.MAX_PROBES_PER_PLAYER + "]", NamedTextColor.GRAY)));
            }
            case REMOVED -> player.sendMessage(Component.text("\u26a1 Probe removed at ", NamedTextColor.YELLOW)
                    .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")", NamedTextColor.WHITE)));
            case FULL -> player.sendMessage(Component.text("\u26a1 Max probes reached (" + ProbeManager.MAX_PROBES_PER_PLAYER + "). Use /rb clear first.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        probeManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
