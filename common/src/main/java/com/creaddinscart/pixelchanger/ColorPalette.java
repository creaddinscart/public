package com.creaddinscart.pixelchanger;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed, numbered palette of colors. The user paints by typing a "color sequence
 * number" (the index) into the editor. Index 0 is the first color.
 */
public final class ColorPalette {

    public record Entry(int argb, String name) {}

    private static final List<Entry> COLORS = new ArrayList<>();

    // ARGB (all opaque). Named so the on-screen legend is readable.
    static {
        add(0xFF1E1E1E, "Black");
        add(0xFFB02E26, "Red");
        add(0xFF5E7C16, "Green");
        add(0xFF735138, "Brown");
        add(0xFF3445B3, "Blue");
        add(0xFF8933AB, "Purple");
        add(0xFF03788E, "Cyan");
        add(0xFF858585, "Light Gray");
        add(0xFF494949, "Gray");
        add(0xFFC76E98, "Pink");
        add(0xFF72B02C, "Lime");
        add(0xFFF5C202, "Yellow");
        add(0xFF3584C9, "Light Blue");
        add(0xFF8B3F92, "Magenta");
        add(0xFFA95A1B, "Orange");
        add(0xFFFDFDFD, "White");
        add(0xFF333333, "Dark Gray");
        add(0xFFAAAAAA, "Silver");
        add(0xFF800000, "Maroon");
        add(0xFF808000, "Olive");
        add(0xFF000080, "Navy");
        add(0xFF008080, "Teal");
        add(0xFF4B0082, "Indigo");
        add(0xFFFFD700, "Gold");
        add(0xFFFF69B4, "Hot Pink");
        add(0xFF87CEEB, "Sky");
        add(0xFF56C596, "Mint");
        add(0xFF262626, "Charcoal");
        add(0xFFFFFFFF, "Snow");
        add(0xFF8B0000, "Crimson");
        add(0xFF228B22, "Forest");
        add(0xFF006994, "Ocean");
    }

    private static void add(int argb, String name) {
        COLORS.add(new Entry(argb, name));
    }

    private ColorPalette() {
    }

    public static int size() {
        return COLORS.size();
    }

    public static Entry get(int index) {
        return COLORS.get(Math.max(0, Math.min(index, COLORS.size() - 1)));
    }

    public static int argb(int index) {
        return get(index).argb();
    }

    public static String name(int index) {
        return get(index).name();
    }

    public static List<Entry> all() {
        return COLORS;
    }
}
