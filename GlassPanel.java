package components;

import java.awt.*;
import javax.swing.JPanel;
import theme.Theme;

/** Base rounded glass panel. UNIT 4: other components extend this class (multi-level inheritance). */
public class GlassPanel extends JPanel {
    private final int arc;

    public GlassPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    public GlassPanel() {                       // UNIT 3: constructor overloading + this(...)
        this(new BorderLayout(), 18);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintGlass(g2, 0, 0, getWidth(), getHeight(), arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
