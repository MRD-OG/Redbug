package mc.mrd_og.redbug;

import org.bukkit.Location;
import org.bukkit.block.data.AnaloguePowerable;

import java.util.LinkedList;

public class Probe {

    public static final int MAX_HISTORY = 60;

    private final Location location;
    private final int scope;
    private final LinkedList<Integer> history = new LinkedList<>();

    public Probe(Location location, int scope) {
        this.location = location.getBlock().getLocation();
        this.scope = scope;
    }

    public void sample() {
        if (!location.isChunkLoaded()) {
            history.addLast(-1);
        } else {
            var block = location.getBlock();
            var data = block.getBlockData();
            int level;
            if (data instanceof AnaloguePowerable powerable) {
                level = powerable.getPower();
            } else {
                level = block.getBlockPower();
            }
            history.addLast(level);
        }
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public Location getLocation() {
        return location;
    }

    public int getScope() {
        return scope;
    }

    public LinkedList<Integer> getHistory() {
        return history;
    }

    public int getCurrentLevel() {
        return history.isEmpty() ? 0 : Math.max(0, history.getLast());
    }

    public boolean isAt(Location loc) {
        return location.getBlockX() == loc.getBlockX()
                && location.getBlockY() == loc.getBlockY()
                && location.getBlockZ() == loc.getBlockZ()
                && location.getWorld().equals(loc.getWorld());
    }

    public String coordString() {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
}
