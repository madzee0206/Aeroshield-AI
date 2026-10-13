package ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import model.Drone;
import physics.FlightModel;
import planning.DroneRoute;
import planning.Plan;
import theme.*;

/**
 * Interactive 3-D bar chart, hand-written with Graphics2D (no libraries).
 *   x axis = drones, z axis = metric (time / energy / distance), y axis = value (each row scaled to its own max).
 * Drag = rotate, mouse wheel = zoom, double-click = reset view. Buttons: Greedy/Baseline, auto-rotate, reset.
 * Maths: yaw/pitch rotation + perspective projection + painter's algorithm with back-face culling and simple lighting.
 */
public class Chart3DPanel extends JPanel {
    private static final String[] METRICS = {"Time (min)", "Energy (Wh)", "Distance (km)"};
    private static final double DEF_YAW = -0.65, DEF_PITCH = 0.55;
    private static final double SX = 1.7, SZ = 1.9, HALF = 0.55, HEIGHT = 3.2, CAM_DIST = 14;

    private final Canvas canvas = new Canvas();
    private final JLabel title = new JLabel("3-D scene");
    private final JButton strategyBtn = Theme.button("SHOW BASELINE", Palette.AMBER);
    private final JButton spinBtn = Theme.button("AUTO-ROTATE", Palette.TEAL);
    private final JButton resetBtn = Theme.button("RESET VIEW", new Color(0x7AA2F7));
    private final javax.swing.Timer spinTimer;

    private Plan greedy, baseline;
    private FlightModel model;
    private boolean showBaseline, spinning;
    private double yaw = DEF_YAW, pitch = DEF_PITCH, zoom = 1.0;

    public Chart3DPanel() {
        super(new BorderLayout());
        setOpaque(false);
        title.setFont(Fonts.HEADING);
        title.setForeground(Palette.TEXT);
        for (JButton b : new JButton[]{strategyBtn, spinBtn, resetBtn}) {
            b.setPreferredSize(new Dimension(b == strategyBtn ? 128 : 108, 28));
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(strategyBtn); buttons.add(spinBtn); buttons.add(resetBtn);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 0, 12));
        header.add(title, BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);

        strategyBtn.addActionListener(e -> { showBaseline = !showBaseline; updateTexts(); canvas.repaint(); });
        resetBtn.addActionListener(e -> resetView());
        spinTimer = new javax.swing.Timer(30, e -> { yaw += 0.012; canvas.repaint(); });
        spinBtn.addActionListener(e -> {
            spinning = !spinning;
            if (spinning) spinTimer.start(); else spinTimer.stop();
            spinBtn.setText(spinning ? "STOP SPIN" : "AUTO-ROTATE");
        });

        MouseAdapter ma = new MouseAdapter() {
            private Point last;
            @Override public void mousePressed(MouseEvent e) { last = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (last == null) return;
                yaw += (e.getX() - last.x) * 0.01;
                pitch = Math.max(0.12, Math.min(1.45, pitch + (e.getY() - last.y) * 0.01));
                last = e.getPoint();
                canvas.repaint();
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                zoom = Math.max(0.5, Math.min(2.6, zoom * Math.pow(1.1, -e.getWheelRotation())));
                canvas.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) resetView(); }
        };
        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(ma);
        canvas.addMouseWheelListener(ma);
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        updateTexts();
    }

    public void set(Plan greedy, Plan baseline, FlightModel model) {
        this.greedy = greedy;
        this.baseline = baseline;
        this.model = model;
        updateTexts();
        canvas.repaint();
    }

    public void setView(double yaw, double pitch, double zoom) {
        this.yaw = yaw; this.pitch = pitch; this.zoom = zoom;
        canvas.repaint();
    }

    private void resetView() { yaw = DEF_YAW; pitch = DEF_PITCH; zoom = 1.0; canvas.repaint(); }

    private Plan current() { return showBaseline ? baseline : greedy; }

    private void updateTexts() {
        Plan p = current();
        title.setText("3-D view: " + (showBaseline ? "Baseline" : "Greedy"));
        strategyBtn.setText(showBaseline ? "SHOW GREEDY" : "SHOW BASELINE");
    }

    // ------------------------------------------------------------------ 3-D canvas
    private static final class Face {
        final double[][] pts;       // 4 world points
        final double[] normal;
        final Color color;
        double depth;
        Face(double[][] pts, double[] normal, Color color) { this.pts = pts; this.normal = normal; this.color = color; }
    }

    private class Canvas extends JComponent {
        private double cx, cy, focal;

        /** world -> camera space (pivot shift, yaw about Y, pitch about X). Larger z = farther. */
        private double[] rot(double x, double y, double z) {
            y -= HEIGHT * 0.45;
            return rotVec(x, y, z);
        }

        private double[] rotVec(double x, double y, double z) {
            double cyaw = Math.cos(yaw), syaw = Math.sin(yaw);
            double x1 = x * cyaw + z * syaw;
            double z1 = -x * syaw + z * cyaw;
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double y2 = y * cp + z1 * sp;
            double z2 = z1 * cp - y * sp;
            return new double[]{x1, y2, z2};
        }

        private Point2D.Double project(double[] r) {
            double f = focal / (CAM_DIST + r[2]);
            return new Point2D.Double(cx + r[0] * f, cy - r[1] * f);
        }

        private Point2D.Double screen(double x, double y, double z) { return project(rot(x, y, z)); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hints(g);
            Plan plan = current();
            int w = getWidth(), h = getHeight();
            g.setFont(Fonts.SMALL);
            g.setColor(Palette.MUTED);
            g.drawString("Drag = rotate   |   Wheel = zoom   |   Double-click = reset   |   each row scaled to its own maximum",
                    18, h - 12);
            if (plan == null || plan.getRoutes().isEmpty() || model == null) { g.dispose(); return; }

            List<DroneRoute> routes = plan.getRoutes();
            int n = routes.size();
            double[][] v = new double[n][3];
            double[] max = {1e-9, 1e-9, 1e-9};
            for (int i = 0; i < n; i++) {
                DroneRoute r = routes.get(i);
                v[i][0] = r.getTotalSeconds() / 60.0;
                v[i][1] = r.getTotalJoules() / 3600.0;
                v[i][2] = model.cellsToMeters(r.getTotalCells()) / 1000.0;
                for (int j = 0; j < 3; j++) max[j] = Math.max(max[j], v[i][j]);
            }
            double scale0 = Math.min(w / (n * SX + 5.0), (h - 60) / 9.5);
            focal = scale0 * CAM_DIST * zoom;
            cx = w / 2.0;
            cy = h / 2.0 + 4;

            drawFloor(g, n);

            // ---- build faces of every bar
            List<Face> faces = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Color base = Palette.droneColor(routes.get(i).getDrone().getId());
                for (int j = 0; j < 3; j++) {
                    double hh = Math.max(v[i][j] / max[j] * HEIGHT, 0.05);
                    double x0 = xAt(i, n) - HALF, x1 = xAt(i, n) + HALF, z0 = zAt(j) - HALF, z1 = zAt(j) + HALF;
                    faces.add(new Face(new double[][]{{x0, hh, z0}, {x1, hh, z0}, {x1, hh, z1}, {x0, hh, z1}}, new double[]{0, 1, 0}, base));
                    faces.add(new Face(new double[][]{{x0, 0, z0}, {x1, 0, z0}, {x1, hh, z0}, {x0, hh, z0}}, new double[]{0, 0, -1}, base));
                    faces.add(new Face(new double[][]{{x0, 0, z1}, {x1, 0, z1}, {x1, hh, z1}, {x0, hh, z1}}, new double[]{0, 0, 1}, base));
                    faces.add(new Face(new double[][]{{x0, 0, z0}, {x0, 0, z1}, {x0, hh, z1}, {x0, hh, z0}}, new double[]{-1, 0, 0}, base));
                    faces.add(new Face(new double[][]{{x1, 0, z0}, {x1, 0, z1}, {x1, hh, z1}, {x1, hh, z0}}, new double[]{1, 0, 0}, base));
                }
            }
            // ---- back-face culling + depth sort (painter's algorithm)
            List<Face> visible = new ArrayList<>();
            for (Face f : faces) {
                double mx = 0, my = 0, mz = 0;
                for (double[] p : f.pts) { mx += p[0]; my += p[1]; mz += p[2]; }
                double[] c = rot(mx / 4, my / 4, mz / 4);
                double[] nr = rotVec(f.normal[0], f.normal[1], f.normal[2]);
                double dot = nr[0] * c[0] + nr[1] * c[1] + nr[2] * (c[2] + CAM_DIST);   // camera sits at z = -CAM_DIST
                if (dot < 0) {
                    f.depth = c[2];
                    visible.add(f);
                }
            }
            visible.sort(Comparator.comparingDouble((Face f) -> f.depth).reversed());

            double[] light = {-0.35, 0.8, -0.5};
            double ll = Math.sqrt(light[0] * light[0] + light[1] * light[1] + light[2] * light[2]);
            for (Face f : visible) {
                double[] nr = rotVec(f.normal[0], f.normal[1], f.normal[2]);
                double lum = 0.45 + 0.55 * Math.max(0, (nr[0] * light[0] + nr[1] * light[1] + nr[2] * light[2]) / ll);
                Path2D poly = new Path2D.Double();
                for (int k = 0; k < 4; k++) {
                    Point2D.Double p = screen(f.pts[k][0], f.pts[k][1], f.pts[k][2]);
                    if (k == 0) poly.moveTo(p.x, p.y); else poly.lineTo(p.x, p.y);
                }
                poly.closePath();
                g.setColor(shade(f.color, lum));
                g.fill(poly);
                g.setColor(new Color(255, 255, 255, 45));
                g.setStroke(new BasicStroke(1f));
                g.draw(poly);
            }

            // ---- labels
            for (int i = 0; i < n; i++) {
                Drone d = routes.get(i).getDrone();
                Point2D.Double p = screen(xAt(i, n), 0, -1.5 * SZ - 0.9);
                pill(g, d.getName(), Palette.droneColor(d.getId()), p.x, p.y, true);
                for (int j = 0; j < 3; j++) {
                    double hh = Math.max(v[i][j] / max[j] * HEIGHT, 0.05);
                    Point2D.Double q = screen(xAt(i, n), hh + 0.35, zAt(j));
                    Theme.drawCentered(g, String.format("%.1f", v[i][j]), Fonts.SMALL, Palette.TEXT, (int) q.x - 25, (int) q.y - 7, 50, 14);
                }
            }
            for (int j = 0; j < 3; j++) {
                Point2D.Double p = screen(n * SX / 2 + 1.0, 0, zAt(j));
                pill(g, METRICS[j], Palette.TEXT, p.x + 8, p.y, false);
            }
            g.dispose();
        }

        /** label with a dark rounded backing so it stays readable on top of bars */
        private void pill(Graphics2D g, String text, Color c, double x, double y, boolean centered) {
            g.setFont(Fonts.LABEL);
            int tw = g.getFontMetrics().stringWidth(text);
            int bx = (int) (centered ? x - tw / 2.0 : x) - 6, by = (int) y - 9;
            g.setColor(new Color(8, 13, 23, 200));
            g.fillRoundRect(bx, by, tw + 12, 18, 10, 10);
            g.setColor(Theme.alpha(c, 130));
            g.drawRoundRect(bx, by, tw + 12, 18, 10, 10);
            g.setColor(c);
            g.drawString(text, bx + 6, by + 13);
        }

        private double xAt(int i, int n) { return (i - (n - 1) / 2.0) * SX; }
        private double zAt(int j) { return (j - 1) * SZ; }

        private void drawFloor(Graphics2D g, int n) {
            double xe = n * SX / 2 + 0.6, ze = 1.5 * SZ + 0.3;
            Path2D floor = new Path2D.Double();
            double[][] c = {{-xe, -ze}, {xe, -ze}, {xe, ze}, {-xe, ze}};
            for (int k = 0; k < 4; k++) {
                Point2D.Double p = screen(c[k][0], 0, c[k][1]);
                if (k == 0) floor.moveTo(p.x, p.y); else floor.lineTo(p.x, p.y);
            }
            floor.closePath();
            g.setColor(Theme.alpha(Palette.TEXT, 14));
            g.fill(floor);
            g.setColor(Theme.alpha(Palette.TEAL, 40));
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < n; i++) {
                Point2D.Double a = screen(xAt(i, n), 0, -ze), b = screen(xAt(i, n), 0, ze);
                g.draw(new Line2D.Double(a, b));
            }
            for (int j = 0; j < 3; j++) {
                Point2D.Double a = screen(-xe, 0, zAt(j)), b = screen(xe, 0, zAt(j));
                g.draw(new Line2D.Double(a, b));
            }
            g.setColor(Theme.alpha(Palette.TEAL, 90));
            g.draw(floor);
        }

        private Color shade(Color c, double lum) {
            return new Color(Math.min(255, (int) (c.getRed() * lum)), Math.min(255, (int) (c.getGreen() * lum)),
                    Math.min(255, (int) (c.getBlue() * lum)));
        }
    }
}
