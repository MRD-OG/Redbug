package mc.md5g22.redbug.objects.node;

import mc.md5g22.redbug.objects.node.visual.NodeHighlight;
import mc.md5g22.redbug.plugin.Redbug;
import mc.md5g22.redbug.util.ColourHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NodeManager {

    private final HashMap<UUID, HashMap<String, Node>> playerNodes = new HashMap<>();

    private final HashMap<UUID, NodeType> playerNodeAddModes = new HashMap<>();
    private final HashMap<UUID, String> playerCurrentNodes = new HashMap<>();
    private final Redbug plugin;

    private static NodeManager nodeManager;

    // Singleton is probably more useful to have
    public static NodeManager getInstance(Redbug plugin) {
        if (nodeManager == null) {
            nodeManager = new NodeManager(plugin);
        }

        return nodeManager;
    }

    private NodeManager(Redbug plugin) {
        this.plugin = plugin;
    }

    public void start() {}


    public void stop() {}

    public HashMap<UUID, HashMap<String, Node>> getPlayerNodes() {
        return playerNodes;
    }

    /**
     *
     * @param player
     * @param t
     * @param nodeName
     * @return returns true if successfully toggled, false if there already exists a node with this name
     */
    public boolean toggleNodeAddMode(Player player, NodeType t, String nodeName, int size, String colour) {

        UUID uuid = player.getUniqueId();

        if (!playerNodes.containsKey(uuid)) {
            // Give player initial entry
            playerNodes.put(uuid, new HashMap<>());
            playerNodes.get(uuid).put(nodeName, new Node(t, size, colour));
        } else {
            if (!playerNodes.get(uuid).containsKey(nodeName)) {
                playerNodes.get(uuid).put(nodeName, new Node(t, size, colour));
            } else {
                // Alert user to size change
                if (size != playerNodes.get(uuid).get(nodeName).getSize()) {
                    player.sendMessage(Component.text("[RB]: Cannot change size of pin try /pin remove or /pin add " + nodeName + " " + playerNodes.get(uuid).get(nodeName).getSize() + " <colour>", NamedTextColor.RED));
                }

                playerNodes.get(uuid).get(nodeName).setColour(colour);
            }
        }


        // Toggle the mode
        if (!playerNodeAddModes.containsKey(uuid)) {
            playerNodeAddModes.put(uuid, t);
            playerCurrentNodes.put(uuid, nodeName);
            return true;
        } else {
            playerNodeAddModes.remove(uuid);
            playerCurrentNodes.remove(uuid);
        }

        return false;
    }

    public boolean isInNodeAddMode(UUID uuid) {
        return playerNodeAddModes.containsKey(uuid);
    }

    public String getCurrentNodeBeingModified(UUID uuid) {

        if (!playerCurrentNodes.containsKey(uuid))
            return null;

        if (!playerNodes.containsKey(uuid))
            return null;

        return playerCurrentNodes.get(uuid);
    }

    public NodeType getPlayerNodeAddMode(UUID uuid) {
        if (isInNodeAddMode(uuid)) {
            return playerNodeAddModes.get(uuid);
        }

        return null;
    }

    public NodeAddResult addNodeLocation(UUID uuid, Location location) {

        if (!playerCurrentNodes.containsKey(uuid))
            return NodeAddResult.NULL;

        if (!playerNodes.containsKey(uuid))
            return NodeAddResult.NULL;

        String currentNodeName = playerCurrentNodes.get(uuid);

        if (!playerNodes.get(uuid).containsKey(currentNodeName))
            return NodeAddResult.NULL;

        Node currentNode = playerNodes.get(uuid).get(currentNodeName);

        // Prioritise removing over node locations being full
        // Don't soft-lock player at full node
        if (currentNode.containsLocation(location) != -1) {
            currentNode.removeLocationHighlight(plugin, location);
            currentNode.removeLocation(location);
            return NodeAddResult.REMOVED;
        }

        if (currentNode.isFull()) {
            return NodeAddResult.FULL;
        }

        currentNode.addLocation(location);
        Display d = NodeHighlight.getNodeHighlight(plugin).highlight(location.getBlock(), ColourHelper.parseColor(currentNode.getColour()));
        return NodeAddResult.ADDED;
    }


    public void cleanupPlayer(UUID uuid) {
        playerNodeAddModes.remove(uuid);
    }

    public List<String> getPlayerNodeNames(UUID uuid) {

        if (playerNodes.containsKey(uuid)) {

            List<String> names = playerNodes.get(uuid).keySet().stream().toList();

            if (!names.isEmpty()) {
                return names;
            }
        }

        return List.of("name");

    }

    public List<String> getPlayerNodeNamesRaw(UUID uuid) {

        if (playerNodes.containsKey(uuid)) {

            List<String> names = playerNodes.get(uuid).keySet().stream().toList();

            if (!names.isEmpty()) {
                return names;
            }
        }

        return new ArrayList<>();

    }

    public boolean removeNode(UUID uuid, String nodeName) {

        if (playerNodes.containsKey(uuid)) {

            if (playerNodes.get(uuid).containsKey(nodeName)) {
                playerNodes.get(uuid).get(nodeName).removeLocationHighlights(plugin);
                playerNodes.get(uuid).remove(nodeName);
                return true;
            }
        }

        return false;
    }

    public void removeAllNodes(UUID uuid) {
        List<String> nodeNames = getPlayerNodeNames(uuid);

        for (String name : nodeNames) {
            nodeManager.removeNode(uuid, name);
        }
    }

    public void removeAllNodes(UUID uuid, NodeType type) {

        List<String> nodeNames = getPlayerNodeNames(uuid);

        for (String name : nodeNames) {
            // todo: add null check for .get(uuid)
            if (playerNodes.get(uuid).get(name).getType() == type)
                nodeManager.removeNode(uuid, name);
        }

    }

    public int getNodeSize(UUID uuid, String nodeName) {
        if (playerNodes.containsKey(uuid)) {
            if (playerNodes.get(uuid).containsKey(nodeName)) {
                return playerNodes.get(uuid).get(nodeName).getSize();
            }
        }

        return -1;
    }
    public class TickTask extends BukkitRunnable {
        int remaining;
        Block leverBlock;
        Redbug plugin;

        public TickTask(int remaining, Block leverBlock, Redbug plugin) {
            this.remaining = remaining;
            this.leverBlock = leverBlock;
            this.plugin = plugin;
        }

        @Override
        public void run() {
            if (remaining > 0) {
                new TickTask(remaining - 1, leverBlock, plugin).runTaskLater(plugin, 1L);
                return;
            }

            if (leverBlock.getBlockData() instanceof Switch sw) {
                setLeverPowered(leverBlock, sw, false);
            }
        }

    }


    public boolean pinOperation(Player player, String nodeName, String value, boolean toggle, int duration) {

        UUID uuid = player.getUniqueId();

        if (!playerNodes.containsKey(uuid))
            return false;

        Node node = playerNodes.get(uuid).get(nodeName);
        if (node == null)
            return false;

        ArrayList<Location> locations = node.getLocations();
        for (Location l : locations) {
            Block block = l.getBlock();
            if (block.getType() != Material.LEVER) {
                player.sendMessage(Component.text("[RB]: Pin " + nodeName + " missing lever at ", NamedTextColor.RED)
                        .append(Component.text("(" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")", NamedTextColor.WHITE)));
                return false;
            }
        }

        for (int i = 0; i < locations.size(); i++) {
            Block block = locations.get(i).getBlock();
            Switch lever = (Switch) block.getBlockData();
            boolean powered = toggle ? !lever.isPowered() : value.charAt(i) == '1';

            setLeverPowered(block, lever, powered);

            if (!toggle && duration > -1) {
                new TickTask(duration, block, plugin).runTask(plugin);
            }
        }

        return true;
    }

    private static void setLeverPowered(Block leverBlock, Switch lever, boolean powered) {
        lever.setPowered(powered);
        // applyPhysics=true triggers vanilla neighborChanged on the lever's 6 cardinals
        // (covering the lever itself, adjacent repeaters/dust, and the attached block).
        leverBlock.setBlockData(lever, true);

        // The attached block may not change state (e.g. a stone block carrying strong power),
        // so vanilla doesn't propagate past it. Manually fire updateNeighborsAt on it so its
        // own 6 cardinals (e.g. dust on the far side) recompute.
        Block attached = getAttachedBlock(leverBlock);
        if (attached != null) {
            updateNeighborsAt(attached);
        }
    }

    private static void updateNeighborsAt(Block block) {
        CraftBlock craftBlock = (CraftBlock) block;
        ServerLevel level = ((CraftWorld) block.getWorld()).getHandle();
        level.updateNeighborsAt(craftBlock.getPosition(), craftBlock.getNMS().getBlock());
    }
    public static Block getAttachedBlock(Block leverBlock) {
        if (leverBlock.getType() != Material.LEVER) return null;

        Switch sw = (Switch) leverBlock.getBlockData();
        BlockFace face;

        switch (sw.getFace()) {
            case FLOOR:
                face = BlockFace.DOWN;
                break;
            case CEILING:
                face = BlockFace.UP;
                break;
            case WALL:
                // For wall levers, the lever faces *away* from the block it's attached to
                face = sw.getFacing().getOppositeFace();
                break;
            default:
                return null;
        }

        return leverBlock.getRelative(face);
    }


    public boolean playerHasNode(Player player, String nodeName) {
        return getPlayerNodeNamesRaw(player.getUniqueId()).contains(nodeName);
    }

    public NodeType getNodeType(Player player, String nodeName) {
        Node node = getNode(player, nodeName);
        if (node == null)
            return NodeType.NULL;

        return node.getType();
    }


    public Node getNode(Player player, String nodeName) {
        if (!playerHasNode(player, nodeName))
            return null;

        return getPlayerNodes().get(player.getUniqueId()).get(nodeName);
    }
}

