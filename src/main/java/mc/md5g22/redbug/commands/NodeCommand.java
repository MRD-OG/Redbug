package mc.md5g22.redbug.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import mc.md5g22.redbug.objects.node.NodeManager;
import mc.md5g22.redbug.objects.node.NodeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NodeCommand implements BasicCommand {


    protected final NodeManager nodeManager;

    public NodeCommand(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    protected void handleAdd(Player player, String[] args, NodeType type) {

        String nodeName = null;
        int size = 1;
        String colour = "blue";

        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few arguments try /pin help", NamedTextColor.RED));
            return;
        }

        nodeName = args[1];

        if (args.length > 2) {
            try {
                size = Integer.parseInt(args[2]);
                if (size < 0) {
                    player.sendMessage(Component.text("[RB]: Size argument must positive", NamedTextColor.RED));
                    return;
                }
            } catch (Exception ignored){
                player.sendMessage(Component.text("[RB]: Size argument must be an integer", NamedTextColor.RED));
                return;
            }

        }

        if (args.length > 3) {
            colour = args[3];
        }

        if (args.length > 4) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
        }

        if(nodeManager.toggleNodeAddMode(player, type, nodeName, size, colour)) {
            player.sendMessage(Component.text("[RB]: Right click blocks to add to " + type, NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("[RB]: Exiting add mode.", NamedTextColor.GOLD));
        }

    }

    protected void handleRemove(Player player, String[] args, NodeType type) {
        String nodeName = null;

        if (args.length < 2) {
            player.sendMessage(Component.text("[RB]: Too few arguments try /pin help", NamedTextColor.RED));
            return;
        }

        nodeName = args[1];

        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
        }

        if (nodeName != null) {
            if (!nodeManager.removeNode(player.getUniqueId(), nodeName)) {
                player.sendMessage(Component.text("[RB]: [" + type.toString() + "] " + nodeName + " does not exist.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("[RB]: [" + type.toString() + "] " + nodeName + " successfully removed.", NamedTextColor.GOLD));
            }
        }
    }

    protected void handleClear(Player player, String[] args, NodeType type) {

        if (args.length > 1) {
            player.sendMessage(Component.text("[RB]: Too many arguments try /pin help", NamedTextColor.RED));
        }

        nodeManager.removeAllNodes(player.getUniqueId(), type);

        player.sendMessage(Component.text("[RB]: All [" + type.toString() + "S] removed.", NamedTextColor.GOLD));

    }

    protected void handleList(Player player, String[] args, NodeType type) {
        if (args.length > 2) {
            player.sendMessage(Component.text("[RB]: Too many parameters try /pin help", NamedTextColor.RED));
            return;
        }
        // List all Nodes
        if (args.length == 1) {

            List<String> nodeNames = nodeManager.getPlayerNodeNamesRaw(player.getUniqueId());

            if (nodeNames.isEmpty()) {
                player.sendMessage(Component.text("[RB]: You have no active pins try /pin add", NamedTextColor.GOLD));
                return;
            } else {
                TextComponent message = Component.text("[RB]: You have " + nodeNames.size() + " active pin(s): ", NamedTextColor.GOLD);

                for (String name : nodeNames) {
                    message = message.appendNewline().append(Component.text(" -> " + name, NamedTextColor.AQUA));
                }

                player.sendMessage(message);
            }

        } else {
            // List all locations per node

            String nodeName = args[1];

            var allNodes = nodeManager.getPlayerNodes();
            var playerNodes = allNodes.get(player.getUniqueId());

            if (playerNodes == null) {
                player.sendMessage(Component.text("[RB]: [" + type.toString() + "] " + nodeName + " does not exist try /" + type.toString().toLowerCase() + " add", NamedTextColor.RED));
                return;
            }

            var node = playerNodes.get(nodeName);

            if (node == null) {
                player.sendMessage(Component.text("[RB]: [" + type.toString() + "] " + nodeName + " does not exist /" + type.toString().toLowerCase() + " add", NamedTextColor.RED));
                return;
            }

            ArrayList<Location> locations = node.getLocations();

            TextComponent message = Component.text("[RB]: [" + type.toString() + "] " + nodeName + " has " + locations.size() + " input(s): ", NamedTextColor.GOLD);

            for (Location loc : locations) {
                Block block = loc.getBlock();
                message = message.appendNewline()
                        .append(Component.text(" -> ", NamedTextColor.AQUA)
                                .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ") - Value : ", NamedTextColor.WHITE))
                        );

                if (type == NodeType.PIN) {
                    message = message.append(Component.text(((Switch) block.getBlockData()).isPowered() ? "1" : "0", NamedTextColor.WHITE));
                }

                if (type == NodeType.WIRE) {
                    message = message.append(Component.text(isBlockPowered(block) ? "1" : "0", NamedTextColor.WHITE));
                }
            }

            player.sendMessage(message);

        }
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] strings) {

    }

    public boolean isBlockPowered(Block block) {
        BlockData data = block.getBlockData();
        Material type = block.getType();

        // 1. Redstone wire (power level 0–15)
        if (data instanceof RedstoneWire wire) {
            return wire.getPower() > 0;
        }

        // 2. Repeater
        if (data instanceof Repeater repeater) {
            return repeater.isPowered();
        }

        // 3. Comparator
        if (data instanceof Comparator comparator) {
            return comparator.isPowered();
        }

        // 4. Redstone torch (always powered unless burned out)
        if (type == Material.REDSTONE_TORCH) {
            return true;
        }

        // 5. Redstone wall torch (same behavior)
        if (type == Material.REDSTONE_WALL_TORCH) {
            return true;
        }

        // 6. Redstone lamp (powered if receiving power from neighbors)
        if (type == Material.REDSTONE_LAMP) {
            return block.isBlockPowered() || block.isBlockIndirectlyPowered();
        }

        // Fallback: use vanilla redstone logic
        return block.isBlockPowered() || block.isBlockIndirectlyPowered();
    }
}
