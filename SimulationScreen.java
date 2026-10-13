package ui;

import analytics.MetricsCalculator;
import components.GlassPanel;
import components.MetricCard;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import map.CityMapRenderer;
import model.GridCell;
import planning.Plan;
import simulation.Scenario;
import simulation.SimulationEngine;
import theme.*;

/** Main screen: metric strip, city map, fleet panel and bottom controls (section 3 of the brief). */
public class SimulationScreen extends JPanel {
    private final Scenario sc;
    private final SimulationEngine eng;
    private final CityMapRenderer renderer = new CityMapRenderer();
    private final MapPanel mapPanel = new MapPanel();
    private final FleetPanel fleet;
    private final MetricCard distCard = new MetricCard("TOTAL DISTANCE", "km", Palette.TEAL);
    private final MetricCard etaCard = new MetricCard("EST. COMPLETION", "min", Palette.AMBER);
    private final MetricCard coverCard = new MetricCard("REPORT COVERAGE", "%", Palette.TEAL);
    private final MetricCard timeCard = new MetricCard("SIM CLOCK", "", Palette.TEXT);
    private final JButton startBtn = Theme.button("START", Palette.TEAL);
    private final JButton pauseBtn = Theme.button("PAUSE", Palette.AMBER);
    private final JButton resumeBtn = Theme.button("RESUME", Palette.TEAL);
    private final JButton resetBtn = Theme.button("RESET", Palette.DANGER);
    private final JButton newBtn = Theme.button("NEW SCENARIO", new Color(0x7AA2F7));
    private final JButton replanBtn = Theme.button("RE-PLAN", new Color(0x7AA2F7));
    private final JSlider speed = new JSlider(1, 16, 2);
    private final JLabel speedLabel = new JLabel("2x");
    private final javax.swing.Timer timer;                       // Swing timer drives the animation
    private long lastNanos = System.nanoTime();
    private Plan cachedPlan;
    private MetricsCalculator.Metrics cached;

    public SimulationScreen(Scenario sc, SimulationEngine eng) {
        super(new BorderLayout(12, 12));
        this.sc = sc;
        this.eng = eng;
        this.fleet = new FleetPanel(sc);
        setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(distCard); cards.add(etaCard); cards.add(coverCard); cards.add(timeCard);
        add(cards, BorderLayout.NORTH);

        GlassPanel mapHolder = new GlassPanel(new BorderLayout(), 18);
        mapHolder.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        mapHolder.add(mapPanel, BorderLayout.CENTER);
        JLabel hint = new JLabel("  Left-click: add / remove report   |   Right-click: toggle no-fly cell   |   "
                + "diamond = report (size = confidence)   circle = survey zone   dashed line = route");
        hint.setFont(Fonts.SMALL);
        hint.setForeground(Palette.MUTED);
        mapHolder.add(hint, BorderLayout.SOUTH);
        add(mapHolder, BorderLayout.CENTER);
        add(fleet, BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);

        timer = new javax.swing.Timer(33, e -> {
            long now = System.nanoTime();
            double dt = Math.min(0.1, (now - lastNanos) / 1e9);
            lastNanos = now;
            eng.tick(dt);
            refresh();
        });
        timer.start();
    }

    private JComponent buildControls() {
        GlassPanel bar = new GlassPanel(new FlowLayout(FlowLayout.LEFT, 10, 10), 18);
        startBtn.addActionListener(e -> eng.start());
        pauseBtn.addActionListener(e -> eng.pause());
        resumeBtn.addActionListener(e -> eng.resume());
        resetBtn.addActionListener(e -> eng.reset());
        newBtn.setPreferredSize(new Dimension(130, 34));
        newBtn.addActionListener(e -> {
            eng.reset();
            sc.newScenario(sc.getSeed() + 1);
            sc.replanAsync(this::repaintLater);
        });
        replanBtn.addActionListener(e -> {
            if (eng.isEditable()) sc.replanAsync(this::repaintLater);
        });
        speed.setOpaque(false);
        speed.setPreferredSize(new Dimension(140, 28));
        speed.addChangeListener(e -> {
            eng.setSpeed(speed.getValue());
            speedLabel.setText(speed.getValue() + "x");
        });
        eng.setSpeed(speed.getValue());
        JLabel sl = new JLabel("SPEED");
        sl.setFont(Fonts.LABEL);
        sl.setForeground(Palette.MUTED);
        speedLabel.setFont(Fonts.LABEL);
        speedLabel.setForeground(Palette.TEAL);
        bar.add(startBtn); bar.add(pauseBtn); bar.add(resumeBtn); bar.add(resetBtn);
        bar.add(Box.createHorizontalStrut(18));
        bar.add(sl); bar.add(speed); bar.add(speedLabel);
        bar.add(Box.createHorizontalStrut(18));
        bar.add(newBtn); bar.add(replanBtn);
        return bar;
    }

    private void repaintLater() {
        SwingUtilities.invokeLater(this::refresh);       // back onto the Swing thread
    }

    public void refresh() {
        Plan p = sc.getActivePlan();
        if (p != null && p != cachedPlan) {
            cachedPlan = p;
            cached = MetricsCalculator.forPlan(p, sc.getReports().size(), sc.getModel(), sc.getMap().getBase());
        }
        if (cached != null) {
            double km = eng.getState() == SimulationEngine.State.READY
                    ? cached.getDistanceKm() : MetricsCalculator.travelledKm(sc.getDrones(), sc.getModel());
            distCard.setValue(String.format("%.2f", km));
            etaCard.setValue(String.format("%.1f", cached.getCompletionMin()));
            coverCard.setValue(String.format("%.0f", cached.getCoveragePct()));
        }
        int t = (int) eng.getSimTime();
        timeCard.setValue(String.format("%02d:%02d", t / 60, t % 60));
        SimulationEngine.State s = eng.getState();
        startBtn.setEnabled(s == SimulationEngine.State.READY && p != null);
        pauseBtn.setEnabled(s == SimulationEngine.State.RUNNING);
        resumeBtn.setEnabled(s == SimulationEngine.State.PAUSED);
        newBtn.setEnabled(s == SimulationEngine.State.READY || s == SimulationEngine.State.FINISHED);
        replanBtn.setEnabled(s == SimulationEngine.State.READY);
        fleet.refresh();
        mapPanel.repaint();
    }

    /** Custom-painted map with mouse interaction (FR-01 / FR-02). */
    private class MapPanel extends JPanel {
        private GridCell hover;

        MapPanel() {
            setOpaque(false);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = renderer.cellAt(sc, e.getX(), e.getY()); }
                @Override public void mouseExited(MouseEvent e) { hover = null; }
                @Override public void mousePressed(MouseEvent e) { click(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void click(MouseEvent e) {
            GridCell g = renderer.cellAt(sc, e.getX(), e.getY());
            if (g == null) return;
            if (!eng.isEditable()) {
                sc.getLog().add(eng.getSimTime(), "Reset the simulation before editing the map");
                return;
            }
            try {
                if (SwingUtilities.isRightMouseButton(e)) {
                    sc.toggleRestricted(g);
                } else {
                    sc.toggleReport(g);
                }
                sc.replanAsync(SimulationScreen.this::repaintLater);
            } catch (IllegalArgumentException ex) {            // unchecked exception from the model
                sc.getLog().add(0, "Rejected: " + ex.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            renderer.paint(g2, sc, getWidth(), getHeight(), hover);
            g2.dispose();
        }
    }
}
