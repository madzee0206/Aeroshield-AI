package theme;

import java.awt.Font;

/** Shared fonts (UNIT 3: static final fields). */
public final class Fonts {
    private Fonts() { }
    public static final Font TITLE   = new Font("SansSerif", Font.BOLD, 18);
    public static final Font HEADING = new Font("SansSerif", Font.BOLD, 13);
    public static final Font LABEL   = new Font("SansSerif", Font.BOLD, 11);
    public static final Font BODY    = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font SMALL   = new Font("SansSerif", Font.PLAIN, 10);
    public static final Font MONO    = new Font("Monospaced", Font.PLAIN, 11);
    public static final Font BIG     = new Font("SansSerif", Font.BOLD, 24);
}
