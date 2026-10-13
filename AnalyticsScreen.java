package ui;

import analytics.MetricsCalculator;
import analytics.MetricsCalculator.Metrics;
import components.GlassPanel;
import components.MetricCard;
import java.awt.*;
import javax.swing.*;
import planning.Plan;
import simulation.Scenario;
import theme.*;

/** FR-08: metric cards, grouped bar chart (greedy vs baseline) and a custom 3-D bar chart. */
public class AnalyticsScreen extends JPanel {
    private final Scenario sc;
    private final MetricCard[] cards = {
        new MetricCard("TOTAL DISTANCE", "km", Palette.TEAL),
        new MetricCard("COMPLETION TIME", "min", Palette.AMBER),
        new MetricCard("ENERGY USED", "Wh", Palette.TEAL),
        new MetricCard("REPORT COVERAGE", "%", Palette.TEAL),
        new MetricCard("ROUTE OVERLAP", "%", Palette.AMBER),
        new MetricCard("FLAGGED ZONES", "", Palette.DANGER)
    };
    private final GroupedBarChart barChart = new GroupedBarChart();
    private final Chart3DPanel chart3d = new Chart3DPanel();
    private final JLabel summary = new JLabel(" ");

    public AnalyticsScreen(Scenario sc) {
        super(new BorderLayout(12, 12));
        this.sc = sc;
        setOpaque(false);
        JPanel top = new JPanel(new GridLayout(1, cards.length, 10, 0));
        top.setOpaque(false);
        for (MetricCard c : cards) top.add(c);
        add(top, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        charts.setOpaque(false);
        GlassPanel a = new GlassPanel(new BorderLayout(), 18);
        a.add(barChart);
        GlassPanel b = new GlassPanel(new BorderLayout(), 18);
        b.add(chart3d);
        charts.add(a);
        charts.add(b);
        add(charts, BorderLayout.CENTER);

        GlassPanel foot = new GlassPanel(new BorderLayout(), 14);
        foot.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        summary.setFont(Fonts.BODY);
        summary.setForeground(Palette.TEXT);
        foot.add(summary);
        add(foot, BorderLayout.SOUTH);
    }

    public void refresh() {
        Plan opt = sc.getOptimizedPlan(), base = sc.getBaselinePlan();
        if (opt == null || base == null) return;
        int n = sc.getReports().size();
        Metrics mo = MetricsCalculator.forPlan(opt, n, sc.getModel(), sc.getMap().getBase());
        Metrics mb = MetricsCalculator.forPlan(base, n, sc.getModel(), sc.getMap().getBase());
        Metrics shown = sc.isUseBaseline() ? mb : mo;
        cards[0].setValue(String.format("%.2f", shown.getDistanceKm()));
        cards[1].setValue(String.format("%.1f", shown.getCompletionMin()));
        cards[2].setValue(String.format("%.1f", shown.getEnergyWh()));
        cards[3].setValue(String.format("%.0f", shown.getCoveragePct()));
        cards[4].setValue(String.format("%.0f", shown.getOverlapPct()));
        cards[5].setValue(String.valueOf(shown.getInfeasibleZones()));

        barChart.set(new String[]{"Distance km", "Time min", "Energy Wh", "Coverage %", "Overlap %", "Flagged"},
                new double[]{mo.getDistanceKm(), mo.getCompletionMin(), mo.getEnergyWh(), mo.getCoveragePct(),
                        mo.getOverlapPct(), mo.getInfeasibleZones()},
                new double[]{mb.getDistanceKm(), mb.getCompletionMin(), mb.getEnergyWh(), mb.getCoveragePct(),
                        mb.getOverlapPct(), mb.getInfeasibleZones()},
                opt.getStrategyName(), base.getStrategyName());
        chart3d.set(opt, base, sc.getModel());

        double gain = mb.getCompletionMin() <= 0 ? 0 : 100.0 * (mb.getCompletionMin() - mo.getCompletionMin()) / mb.getCompletionMin();
        summary.setText(String.format("Greedy vs baseline:  completion %.1f vs %.1f min (%+.0f%%),  coverage %.0f%% vs %.0f%%,  "
                        + "workload imbalance (std-dev) %.1f vs %.1f min.   Showing cards for: %s",
                mo.getCompletionMin(), mb.getCompletionMin(), -gain, mo.getCoveragePct(), mb.getCoveragePct(),
                mo.getBalanceMin(), mb.getBalanceMin(), sc.getActivePlan().getStrategyName()));
    }

    // ------------------------------------------------------------------ grouped bar chart
    private static class GroupedBarChart extends JComponent {
        private String[] groups = new String[0];
        private double[] a = new double[0], b = new double[0];
        private String nameA = "", nameB = "";

        void set(String[] groups, double[] a, double[] b, String nameA, String nameB) {
            this.groups = groups; this.a = a; this.b = b; this.nameA = nameA; this.nameB = nameB;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hints(g);
            g.setFont(Fonts.HEADING);
            g.setColor(Palette.TEXT);
            g.drawString("Greedy vs baseline (each group scaled to its own maximum)", 18, 28);
            // legend
            g.setColor(Palette.TEAL); g.fillRoundRect(18, 40, 10, 10, 3, 3);
            g.setFont(Fonts.SMALL); g.setColor(Palette.MUTED); g.drawString(nameA, 33, 49);
            int lx = 50 + g.getFontMetrics().stringWidth(nameA);
            g.setColor(Palette.AMBER); g.fillRoundRect(lx, 40, 10, 10, 3, 3);
            g.setColor(Palette.MUTED); g.drawString(nameB, lx + 15, 49);

            int n = groups.length;
            if (n == 0) { g.dispose(); return; }
            int left = 20, right = getWidth() - 20, top = 70, bottom = getHeight() - 36;
            double slot = (right - left) / (double) n;
            double barW = Math.min(30, slot / 3.2);
            g.setColor(Palette.GRID);
            g.drawLine(left, bottom, right, bottom);
            for (int i = 0; i < n; i++) {
                double max = Math.max(Math.max(a[i], b[i]), 1e-9);
                double cx = left + slot * (i + 0.5);
                drawBar(g, cx - barW - 2, bottom, barW, (bottom - top) * a[i] / max, Palette.TEAL, a[i]);
                drawBar(g, cx + 2, bottom, barW, (bottom - top) * b[i] / max, Palette.AMBER, b[i]);
                Theme.drawCentered(g, groups[i], Fonts.SMALL, Palette.MUTED, (int) (cx - slot / 2), bottom + 4, (int) slot, 20);
            }
            g.dispose();
        }

        private void drawBar(Graphics2D g, double x, double base, double w, double h, Color c, double value) {
            h = Math.max(h, 2);
            g.setPaint(new GradientPaint((float) x, (float) (base - h), c, (float) x, (float) base, Theme.alpha(c, 70)));
            g.fill(new java.awt.geom.RoundRectangle2D.Double(x, base - h, w, h, 6, 6));
            Theme.drawCentered(g, String.format(value == Math.rint(value) ? "%.0f" : "%.1f", value), Fonts.SMALL,
                    Palette.TEXT, (int) (x - 10), (int) (base - h - 16), (int) (w + 20), 14);
        }
    }
}
