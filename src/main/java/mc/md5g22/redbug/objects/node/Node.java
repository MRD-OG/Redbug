package mc.md5g22.redbug.objects.node;

import mc.md5g22.redbug.plugin.Redbug;
import mc.md5g22.redbug.objects.node.visual.NodeHighlight;
import org.bukkit.Location;
import org.bukkit.block.data.AnaloguePowerable;

import java.util.ArrayList;
import java.util.LinkedList;

public class Node {

    private static final int MAX_HISTORY = 60;

    private final int size;
    private String colour;
    private final NodeType type;
    private final ArrayList<Location> locations = new ArrayList<>();
    private final ArrayList<LinkedList<Integer>> histories = new ArrayList<>();

    public Node(NodeType type, int size, String colour) {
        this.type = type;
        this.size = size;
        this.colour = colour;
    }

    public boolean isModifiable() {
        return this.type == NodeType.PIN;
    }

    public boolean isFull() {
        return locations.size() == this.size;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }
    public boolean addLocation(Location location) {

        if (isFull()) {
            return false;
        }

        locations.add(location);
        histories.add(new LinkedList<>());

        return true;
    }

    public void sample() {

        for (int i = 0; i < locations.size(); i++) {

            Location l = locations.get(i);
            LinkedList<Integer> h = histories.get(i);

            if (!l.isChunkLoaded()) {
                h.addLast(-1);
            } else {
                var block = l.getBlock();
                var data = block.getBlockData();
                int level;
                if (data instanceof AnaloguePowerable powerable) {
                    level = powerable.getPower();
                } else {
                    level = block.getBlockPower();
                }
                h.addLast(Math.clamp(level, 0, 1));
            }
            while (h.size() > MAX_HISTORY) {
                h.removeFirst();
            }
        }
    }

    public ArrayList<Location> getLocations() {
        return locations;
    }

    public ArrayList<LinkedList<Integer>> getHistories() {
        return histories;
    }

    public NodeType getType() {
        return type;
    }

    public String getColour() {
        return colour;
    }

    public ArrayList<Integer> getCurrentLevels() {
        ArrayList<Integer> result = new ArrayList<>();
        for (LinkedList<Integer> h : histories) {
            result.add(h.getLast());
        }
        return result;
    }

    public int getSize() {
        return size;
    }

    public int containsLocation(Location loc) {

        for (int i = 0; i < locations.size(); i++) {
            Location l = locations.get(i);
            boolean match = l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ();

            if (match) {
                return i;
            }
        }

        return -1;
    }

    public boolean removeLocation(Location loc) {

        int idx = containsLocation(loc);

        if (idx == -1) {
            return false;
        }

        locations.remove(idx);
        histories.remove(idx);

        return true;
    }

    public boolean hasLocations() {
        return !locations.isEmpty();
    }

    public void removeLocationHighlights(Redbug plugin) {

        NodeHighlight n = NodeHighlight.getNodeHighlight(plugin);

        for (Location l : locations) {
            n.remove(l.getBlock());
        }
    }

    public void removeLocationHighlight(Redbug plugin, Location location) {
        NodeHighlight.getNodeHighlight(plugin).remove(location.getBlock());
    }

}
