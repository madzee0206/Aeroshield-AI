package theme;

import java.awt.*;                      // wildcard import
import javax.swing.JButton;             // single-class import
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/** Painting helpers shared by every screen. */
public final class Theme {
    private Theme() { }

    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Rounded "glass" surface: translucent fill, top highlight, thin border. */
    public static void paintGlass(Graphics2D g, int x, int y, int w, int h, int arc) {
        hints(g);
        g.setColor(alpha(Palette.GLASS, 220));
        g.fillRoundRect(x, y, w - 1, h - 1, arc, arc);
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 22), x, y + h, new Color(255, 255, 255, 0)));
        g.fillRoundRect(x, y, w - 1, h - 1, arc, arc);
        g.setColor(Palette.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
    }

    public static void drawCentered(Graphics2D g, String text, Font f, Color c, int x, int y, int w, int h) {
        g.setFont(f);
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    /** Flat glass-style button created with an anonymous inner class. */
    public static JButton button(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                hints(g);
                Color c = isEnabled() ? accent : Palette.MUTED;
                int a = !isEnabled() ? 12 : (getModel().isRollover() ? 80 : 38);
                g.setColor(alpha(c, a));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.setColor(alpha(c, isEnabled() ? 200 : 80));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                drawCentered(g, getText(), Fonts.LABEL, c, 0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(96, 34));
        return b;
    }

    public static void styleInput(JComponent c) {
        c.setFont(Fonts.BODY);
        c.setForeground(Palette.TEXT);
        c.setBackground(new Color(0x0B1320));
    }
}
