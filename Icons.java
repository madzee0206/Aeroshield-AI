package theme;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** Vector icons drawn with Graphics2D (no image files needed). */
public final class Icons {
    private Icons() { }

    public static void drone(Graphics2D g, double cx, double cy, double size, Color c) {
        double a = size * 0.55;
        g.setStroke(new BasicStroke((float) Math.max(1.5, size * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[][] sign = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};          // 2-D array of arm directions
        for (int[] s : sign) {
            double rx = cx + s[0] * a, ry = cy + s[1] * a;
            g.setColor(c);
            g.draw(new Line2D.Double(cx, cy, rx, ry));
            g.setColor(Theme.alpha(c, 110));
            g.fill(new Ellipse2D.Double(rx - size * 0.28, ry - size * 0.28, size * 0.56, size * 0.56));
        }
        g.setColor(Palette.NAVY);
        g.fill(new Ellipse2D.Double(cx - size * 0.28, cy - size * 0.28, size * 0.56, size * 0.56));
        g.setColor(c);
        g.draw(new Ellipse2D.Double(cx - size * 0.28, cy - size * 0.28, size * 0.56, size * 0.56));
    }

    public static void base(Graphics2D g, double cx, double cy, double size, Color c) {
        Path2D hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(60 * i - 30);
            double x = cx + Math.cos(ang) * size, y = cy + Math.sin(ang) * size;
            if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
        }
        hex.closePath();
        g.setColor(Theme.alpha(c, 70));
        g.fill(hex);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(hex);
        Theme.drawCentered(g, "H", Fonts.LABEL, c, (int) (cx - size), (int) (cy - size), (int) (size * 2), (int) (size * 2));
    }

    /** Report marker: diamond whose size grows with confidence. */
    public static void report(Graphics2D g, double cx, double cy, double size, Color c, double confidence) {
        double r = size * (0.35 + 0.35 * confidence);
        Path2D d = new Path2D.Double();
        d.moveTo(cx, cy - r); d.lineTo(cx + r, cy); d.lineTo(cx, cy + r); d.lineTo(cx - r, cy); d.closePath();
        g.setColor(Theme.alpha(c, 200));
        g.fill(d);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1f));
        g.draw(d);
    }
}
