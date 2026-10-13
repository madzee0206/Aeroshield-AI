package components;

import java.awt.*;
import theme.*;

/** JComponent -> JPanel -> GlassPanel -> MetricCard : multi-level inheritance. */
public class MetricCard extends GlassPanel {
    private final String title;
    private final String unit;
    private final Color accent;
    private String value = "--";

    public MetricCard(String title, String unit, Color accent) {
        super(new BorderLayout(), 16);
        this.title = title;
        this.unit = unit;
        this.accent = accent;
        setPreferredSize(new Dimension(170, 74));
    }

    public void setValue(String v) {
        if (!v.equals(value)) {
            value = v;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                       // UNIT 4: super call
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        g2.setFont(Fonts.SMALL);
        g2.setColor(Palette.MUTED);
        g2.drawString(title, 14, 22);
        g2.setFont(Fonts.BIG);
        g2.setColor(accent);
        g2.drawString(value, 14, 54);
        if (!unit.isEmpty()) {
            int w = g2.getFontMetrics().stringWidth(value);
            g2.setFont(Fonts.BODY);
            g2.setColor(Palette.MUTED);
            g2.drawString(unit, 18 + w, 54);
        }
        g2.dispose();
    }
}
