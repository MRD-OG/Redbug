package mc.md5g22.redbug.listeners;

import mc.md5g22.redbug.objects.node.NodeAddResult;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.objects.node.NodeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;
import java.util.UUID;

public class NodePlacerListener implements Listener {

    private final NodeManager nodeManager;

    public NodePlacerListener(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!nodeManager.isInNodeAddMode(player.getUniqueId())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();

        String nodeName = nodeManager.getCurrentNodeBeingModified(uuid);
        NodeType nodeType = nodeManager.getPlayerNodeAddMode(uuid);

        Set<Material> VALID_WIRE_BLOCKS = Set.of(
                Material.REPEATER,
                Material.COMPARATOR,
                Material.REDSTONE_WIRE,
                Material.REDSTONE_LAMP,
                Material.REDSTONE_TORCH,
                Material.REDSTONE_WALL_TORCH
        );

        switch (nodeType) {
            case PIN -> {
                if (block.getType() != Material.LEVER) return;
            }
            case WIRE -> {
                if (!VALID_WIRE_BLOCKS.contains(block.getType())) return;
            }
        }

        NodeAddResult result = nodeManager.addNodeLocation(uuid, block.getLocation());

        switch (result) {
            case ADDED -> {
                player.sendMessage(
                        Component.text("[RB]: Node added to [" + nodeType + "] " + nodeName + " at ", NamedTextColor.GREEN)
                .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")", NamedTextColor.WHITE))
                );
            }
            case REMOVED -> {
                player.sendMessage(
                        Component.text("[RB]: Node removed from [" + nodeType + "] " + nodeName + " at ", NamedTextColor.YELLOW)
                .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")", NamedTextColor.WHITE))
                );
            }
            case FULL -> {
                player.sendMessage(
                        Component.text("[RB]: Max nodes reaches for [" + nodeType + "] " + nodeName, NamedTextColor.RED)
                );
            }
            case NULL -> {
                player.sendMessage(
                        Component.text("[RB]: Node... somehow doesn't exist [" + nodeType + "] " + nodeName, NamedTextColor.RED)
                );
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        nodeManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
