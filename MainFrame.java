package ui;

import components.GlassPanel;
import components.StatusBadge;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import model.Drone;
import model.SightingReport;
import planning.DroneRoute;
import planning.Plan;
import simulation.Scenario;
import simulation.SimulationEngine;
import theme.*;

/** Window: sidebar + top bar + CardLayout of screens (section 3 of the brief). */
public class MainFrame extends JFrame {
    private final Scenario sc = new Scenario(2);
    private final SimulationEngine eng = new SimulationEngine(sc);
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final Map<String, NavButton> navs = new LinkedHashMap<>();
    private final SimulationScreen simScreen = new SimulationScreen(sc, eng);
    private final AnalyticsScreen analytics = new AnalyticsScreen(sc);
    private final TableScreen fleetScreen;
    private final TableScreen reportsScreen;
    private final SettingsScreen settings;
    private final StatusBadge badge = new StatusBadge("SYSTEM ONLINE", Palette.TEAL);
    private final JLabel dispatch = new JLabel();
    private final JLabel scenarioName = new JLabel();
    private String current = "SIMULATION";

    public MainFrame() {
        super("AeroShield AI - Drone Swarm Simulation");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        fleetScreen = new TableScreen("FLEET", new String[]{"Drone", "Model", "Status", "Battery %", "Assigned zones", "Distance km"},
                this::fleetRows);
        reportsScreen = new TableScreen("INCIDENT REPORTS", new String[]{"ID", "Cell (row,col)", "Confidence", "Time", "Cluster / zone"},
                this::reportRows);
        settings = new SettingsScreen(sc, eng, () -> simScreen.refresh());

        center.setOpaque(false);
        center.add(wrap(simScreen), "SIMULATION");
        center.add(fleetScreen, "FLEET");
        center.add(reportsScreen, "REPORTS");
        center.add(analytics, "ANALYTICS");
        center.add(settings, "SETTINGS");

        JPanel root = new BackgroundPanel();
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        right.add(buildTopBar(), BorderLayout.NORTH);
        right.add(center, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        setSize(1400, 880);
        setMinimumSize(new Dimension(1150, 740));
        setLocationRelativeTo(null);

        javax.swing.Timer slow = new javax.swing.Timer(200, e -> refreshChrome());
        slow.start();
        show("SIMULATION");
        sc.replanAsync(() -> SwingUtilities.invokeLater(simScreen::refresh));     // plan on a background thread
    }

    private JComponent wrap(JComponent c) { return c; }

    // ------------------------------------------------------------------ chrome
    private JComponent buildSidebar() {
        GlassPanel side = new GlassPanel(new BorderLayout(), 20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.hints(g2);
                Icons.drone(g2, 36, 40, 24, Palette.TEAL);
                g2.setFont(Fonts.HEADING);
                g2.setColor(Palette.TEXT);
                g2.drawString("AEROSHIELD", 62, 36);
                g2.setFont(Fonts.SMALL);
                g2.setColor(Palette.TEAL);
                g2.drawString("AI  ·  SWARM CONTROL", 62, 50);
                g2.setColor(Palette.MUTED);
                g2.drawString("synthetic data only", 18, getHeight() - 18);
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(200, 100));
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6));
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(80, 12, 0, 12));
        for (String name : new String[]{"SIMULATION", "FLEET", "REPORTS", "ANALYTICS", "SETTINGS"}) {
            NavButton b = new NavButton(name);
            b.addActionListener(e -> show(name));
            navs.put(name, b);
            nav.add(b);
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(nav, BorderLayout.NORTH);
        side.add(holder, BorderLayout.CENTER);
        return side;
    }

    private JComponent buildTopBar() {
        GlassPanel bar = new GlassPanel(new BorderLayout(), 18);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        scenarioName.setFont(Fonts.TITLE);
        scenarioName.setForeground(Palette.TEXT);
        dispatch.setFont(Fonts.HEADING);
        dispatch.setForeground(Palette.AMBER);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        east.setOpaque(false);
        east.add(dispatch);
        east.add(badge);
        bar.add(scenarioName, BorderLayout.WEST);
        bar.add(east, BorderLayout.EAST);
        return bar;
    }

    private void show(String name) {
        current = name;
        cards.show(center, name);
        for (Map.Entry<String, NavButton> e : navs.entrySet()) {
            e.getValue().setActive(e.getKey().equals(name));
        }
        refreshChrome();
    }

    private void refreshChrome() {
        scenarioName.setText(sc.getName());
        dispatch.setText("DISPATCHES  " + eng.getDispatched());
        if (sc.isPlanning()) {
            badge.set("PLANNING...", Palette.AMBER);
        } else {
            switch (eng.getState()) {
                case RUNNING:  badge.set("SIMULATING", Palette.TEAL); break;
                case PAUSED:   badge.set("PAUSED", Palette.AMBER); break;
                case FINISHED: badge.set("MISSION COMPLETE", new Color(0x9ECE6A)); break;
                default:       badge.set("SYSTEM ONLINE", Palette.TEAL); break;
            }
        }
        switch (current) {
            case "FLEET":     fleetScreen.refresh(); break;
            case "REPORTS":   reportsScreen.refresh(); break;
            case "ANALYTICS": analytics.refresh(); break;
            default: break;
        }
    }

    // ------------------------------------------------------------------ table data
    private Object[][] fleetRows() {
        List<Drone> ds = sc.getDrones();
        Plan plan = sc.getActivePlan();
        Object[][] rows = new Object[ds.size()][];            // 2-D array
        for (int i = 0; i < ds.size(); i++) {
            Drone d = ds.get(i);
            DroneRoute r = plan == null ? null : plan.routeFor(d);
            rows[i] = new Object[]{d.getName(), d.getModelName(), d.getStatus(),
                    String.format("%.0f", d.getBatteryPercent()), r == null ? "-" : r.zoneNames(),
                    String.format("%.2f", sc.getModel().cellsToMeters(d.getDistanceCells()) / 1000.0)};
        }
        return rows;
    }

    private Object[][] reportRows() {
        List<SightingReport> rs = sc.getReports();
        Object[][] rows = new Object[rs.size()][];
        for (int i = 0; i < rs.size(); i++) {
            SightingReport r = rs.get(i);
            rows[i] = new Object[]{r.getId(), r.getCell().toString(), Math.round(r.getConfidence() * 100) + "%",
                    r.getTimestamp(), r.getClusterIndex() < 0 ? "-" : "Zone " + (char) ('A' + r.getClusterIndex())};
        }
        return rows;
    }

    // ------------------------------------------------------------------ inner classes
    private static class BackgroundPanel extends JPanel {
        BackgroundPanel() { super(new BorderLayout(14, 0)); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, Palette.NAVY, getWidth(), getHeight(), new Color(0x0D1626)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.alpha(Palette.TEAL, 10));                       // subtle grid lines
            for (int x = 0; x < getWidth(); x += 40) g2.drawLine(x, 0, x, getHeight());
            for (int y = 0; y < getHeight(); y += 40) g2.drawLine(0, y, getWidth(), y);
            g2.dispose();
        }
    }

    private static class NavButton extends JButton {
        private boolean active;

        NavButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(160, 40));
        }

        void setActive(boolean a) { active = a; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            if (active || getModel().isRollover()) {
                g2.setColor(Theme.alpha(Palette.TEAL, active ? 40 : 18));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            if (active) {
                g2.setColor(Palette.TEAL);
                g2.fillRoundRect(0, 8, 3, getHeight() - 16, 3, 3);
            }
            g2.setFont(Fonts.LABEL);
            g2.setColor(active ? Palette.TEAL : Palette.MUTED);
            g2.drawString(getText(), 18, getHeight() / 2 + 4);
            g2.dispose();
        }
    }
}
