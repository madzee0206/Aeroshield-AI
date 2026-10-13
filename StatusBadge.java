package components;

import java.awt.*;
import javax.swing.JComponent;
import theme.*;

/** Small pill showing a status word with a coloured dot. */
public class StatusBadge extends JComponent {
    private String text;
    private Color color;

    public StatusBadge(String text, Color color) {
        this.text = text;
        this.color = color;
        setPreferredSize(new Dimension(150, 26));
    }

    public void set(String text, Color color) {
        if (!text.equals(this.text) || !color.equals(this.color)) {
            this.text = text;
            this.color = color;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        g2.setColor(Theme.alpha(color, 36));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
        g2.setColor(Theme.alpha(color, 170));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
        g2.setColor(color);
        g2.fillOval(10, getHeight() / 2 - 3, 7, 7);
        Theme.drawCentered(g2, text, Fonts.LABEL, color, 14, 0, getWidth() - 14, getHeight());
        g2.dispose();
    }
}
