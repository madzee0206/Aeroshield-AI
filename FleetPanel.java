package ui;

import components.GlassPanel;
import components.StatusBadge;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import model.Drone;
import model.DroneStatus;
import planning.DroneRoute;
import planning.Plan;
import simulation.Scenario;
import theme.*;

/** Right-hand panel of the simulation screen: drone roster with battery bars + live event feed. */
public class FleetPanel extends GlassPanel {
    private final Scenario sc;
    private final RosterView roster;
    private final JTextArea feed = new JTextArea();
    private int lastLogSize = -1;

    public FleetPanel(Scenario sc) {
        super(new BorderLayout(0, 8), 18);
        this.sc = sc;
        setPreferredSize(new Dimension(320, 100));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("FLEET STATUS");
        title.setFont(Fonts.LABEL);
        title.setForeground(Palette.MUTED);

        roster = new RosterView();
        JScrollPane rs = new JScrollPane(roster);
        rs.setBorder(BorderFactory.createEmptyBorder());
        rs.setOpaque(false);
        rs.getViewport().setOpaque(false);
        rs.setPreferredSize(new Dimension(290, 300));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(rs, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        feed.setEditable(false);
        feed.setLineWrap(true);
        feed.setWrapStyleWord(true);
        feed.setOpaque(false);
        feed.setFont(Fonts.MONO);
        feed.setForeground(Palette.TEXT);
        JScrollPane fs = new JScrollPane(feed);
        fs.setBorder(BorderFactory.createEmptyBorder());
        fs.setOpaque(false);
        fs.getViewport().setOpaque(false);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JLabel ft = new JLabel("EVENT FEED");
        ft.setFont(Fonts.LABEL);
        ft.setForeground(Palette.MUTED);
        bottom.add(ft, BorderLayout.NORTH);
        bottom.add(fs, BorderLayout.CENTER);
        add(bottom, BorderLayout.CENTER);
    }

    public void refresh() {
        roster.repaint();
        int size = sc.getLog().size();
        if (size != lastLogSize) {
            lastLogSize = size;
            StringBuilder sb = new StringBuilder();
            for (String line : sc.getLog().last(120)) {
                sb.append(line).append('\n');
            }
            feed.setText(sb.toString());
            feed.setCaretPosition(feed.getDocument().getLength());
        }
    }

    static Color statusColor(DroneStatus s) {
        switch (s) {
            case FLYING:    return Palette.TEAL;
            case SURVEYING: return Palette.AMBER;
            case RETURNING: return new Color(0x7AA2F7);
            case COMPLETED: return new Color(0x9ECE6A);
            default:        return Palette.MUTED;
        }
    }

    /** Custom-painted list of drones. */
    private class RosterView extends JComponent {
        private static final int ROW = 74;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(280, Math.max(ROW, sc.getDrones().size() * ROW));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hints(g);
            List<Drone> drones = sc.getDrones();
            Plan plan = sc.getActivePlan();
            int w = getWidth();
            for (int i = 0; i < drones.size(); i++) {
                Drone d = drones.get(i);
                int y = i * ROW;
                Color dc = Palette.droneColor(d.getId());
                Theme.paintGlass(g, 0, y + 2, w - 4, ROW - 6, 14);
                Icons.drone(g, 24, y + 26, 18, dc);
                g.setFont(Fonts.HEADING);
                g.setColor(Palette.TEXT);
                g.drawString(d.getName(), 46, y + 24);
                Color sc2 = statusColor(d.getStatus());
                String st = d.getStatus().name();
                g.setFont(Fonts.LABEL);
                int sw = g.getFontMetrics().stringWidth(st);
                g.setColor(Theme.alpha(sc2, 40));
                g.fillRoundRect(w - sw - 30, y + 10, sw + 16, 20, 14, 14);
                g.setColor(sc2);
                g.drawString(st, w - sw - 22, y + 24);

                DroneRoute r = plan == null ? null : plan.routeFor(d);
                g.setFont(Fonts.SMALL);
                g.setColor(Palette.MUTED);
                g.drawString("Zone: " + (r == null ? "-" : r.zoneNames()), 46, y + 41);
                // battery bar
                double pct = d.getBatteryPercent();
                int bx = 46, by = y + 50, bw = w - 100, bh = 8;
                g.setColor(new Color(255, 255, 255, 25));
                g.fillRoundRect(bx, by, bw, bh, 8, 8);
                Color bc = pct < sc.getReservePercent() ? Palette.DANGER : (pct < 50 ? Palette.AMBER : dc);
                g.setColor(bc);
                g.fillRoundRect(bx, by, (int) (bw * pct / 100.0), bh, 8, 8);
                g.setColor(Palette.TEXT);
                g.drawString(Math.round(pct) + "%", bx + bw + 8, by + 8);
            }
            g.dispose();
        }
    }
}
