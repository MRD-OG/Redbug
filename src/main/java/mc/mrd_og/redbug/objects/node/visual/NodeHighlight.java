package mc.mrd_og.redbug.objects.node.visual;

import mc.mrd_og.redbug.plugin.Redbug;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class NodeHighlight {

    private final Redbug plugin;
    private final NamespacedKey key;

    private static NodeHighlight nodeHighlight;

    // Singleton
    public static NodeHighlight getNodeHighlight(Redbug plugin) {
        if (nodeHighlight == null) {
            nodeHighlight = new NodeHighlight(plugin);
        }
        return nodeHighlight;
    }

    private NodeHighlight(Redbug plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "redbug_highlight");
    }

    public Display highlight(Block block, Color color) {
        // Prevent duplicates
        for (Entity e : block.getWorld().getNearbyEntities(block.getLocation(), 0.1, 0.1, 0.1)) {
            if (isHighlightEntity(e)) {
                return (Display) e;
            }
        }

        Location loc = block.getLocation();

        BlockDisplay display = block.getWorld().spawn(loc.add(new Vector(-0.0005f, -0.0005f, -0.0005f)), BlockDisplay.class, d -> {
            d.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());

            // Invisible model
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(1.01f, 1.01f, 1.01f),
                    new Quaternionf()
            ));

            d.setGlowing(true);
            d.setGlowColorOverride(color);
            d.setBrightness(new Display.Brightness(15, 15));

            // Adds pcd tag
            d.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        });


        return display;
    }

    public void remove(Block block) {
        for (Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(new Vector(-0.0005f, -0.0005f, -0.0005f)), 0.1, 0.1, 0.1)) {
            if (isHighlightEntity(e)) {
                e.remove();
            }
        }
    }

    public void clearAll(World world) {
        for (Entity e : world.getEntitiesByClass(BlockDisplay.class)) {
            if (isHighlightEntity(e)) {
                e.remove();
            }
        }
    }

    private boolean isHighlightEntity(Entity e) {
        if (!(e instanceof BlockDisplay)) return false;

        PersistentDataContainer pdc = e.getPersistentDataContainer();
        return pdc.has(key, PersistentDataType.INTEGER);
    }
}
