package theme;

import java.awt.Color;

/**
 * Central colour palette.
 * UNIT 3: final class + private constructor (cannot be instantiated or extended),
 *         static final constants, static methods.
 */
public final class Palette {
    private Palette() { }

    public static final Color NAVY   = new Color(0x080D17);
    public static final Color GLASS  = new Color(0x121C2A);
    public static final Color TEAL   = new Color(0x50E0CE);
    public static final Color AMBER  = new Color(0xE6B85C);
    public static final Color DANGER = new Color(0xE5605A);
    public static final Color TEXT   = new Color(0xE8EEF5);
    public static final Color MUTED  = new Color(0x8A9BB0);
    public static final Color BORDER = new Color(255, 255, 255, 38);
    public static final Color GRID   = new Color(255, 255, 255, 16);

    // UNIT 5: 1-D arrays of objects
    private static final Color[] DRONE_COLORS = hex(0x50E0CE, 0xE6B85C, 0x7AA2F7, 0xF7768E, 0xBB9AF7, 0x9ECE6A);
    private static final Color[] CLUSTER_COLORS = hex(0xFF6B9D, 0xC792EA, 0x82AAFF, 0xFFCB6B, 0x89DDFF, 0xC3E88D, 0xF78C6C, 0xFFEB95);

    private static Color[] hex(int... values) {          // varargs
        Color[] out = new Color[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = new Color(values[i]);
        }
        return out;
    }

    /** Each drone keeps one colour for its icon, route and roster entry. */
    public static Color droneColor(int index) { return DRONE_COLORS[index % DRONE_COLORS.length]; }

    public static Color clusterColor(int index) {
        return index < 0 ? TEXT : CLUSTER_COLORS[index % CLUSTER_COLORS.length];
    }
}
