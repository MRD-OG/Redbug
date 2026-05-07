package mc.md5g22.redbug.util;

import org.bukkit.Color;

import java.util.List;

public class ColourHelper {

    public static final List<String> COLOURS = List.of("red", "blue", "green", "lime", "yellow", "orange", "aqua", "cyan", "purple", "magenta", "pink", "white", "black", "black", "gray", "grey", "maroon", "navy", "teal", "olive");

    public static Color parseColor(String name) {
        if (name == null) return Color.WHITE;
        return switch (name.toLowerCase()) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "lime" -> Color.LIME;
            case "yellow" -> Color.YELLOW;
            case "orange" -> Color.ORANGE;
            case "aqua", "cyan" -> Color.AQUA;
            case "purple", "magenta" -> Color.PURPLE;
            case "pink" -> Color.FUCHSIA;
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "gray", "grey" -> Color.GRAY;
            case "silver" -> Color.SILVER;
            case "maroon" -> Color.MAROON;
            case "navy" -> Color.NAVY;
            case "teal" -> Color.TEAL;
            case "olive" -> Color.OLIVE;
            default -> Color.WHITE; // fallback
        };
    }

}
