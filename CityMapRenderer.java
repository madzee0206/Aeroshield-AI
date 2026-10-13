package map;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import model.*;
import planning.DroneRoute;
import planning.Plan;
import simulation.Scenario;
import theme.Icons;
import theme.Palette;
import theme.Theme;
import theme.Fonts;
import static theme.Palette.TEXT;          // static import

/** Draws the whole city with Graphics2D (FR-01 / FR-03 / FR-05). Geometry is recomputed on every paint. */
public class CityMapRenderer {
    private double cell = 20, ox, oy;

    /** Converts a mouse position to a grid cell (null when outside the map). */
    public GridCell cellAt(Scenario sc, int x, int y) {
        int c = (int) Math.floor((x - ox) / cell);
        int r = (int) Math.floor((y - oy) / cell);
        return sc.getMap().inBounds(r, c) ? new GridCell(r, c) : null;
    }

    private double cx(double col) { return ox + (col + 0.5) * cell; }
    private double cy(double row) { return oy + (row + 0.5) * cell; }

    public void paint(Graphics2D g, Scenario sc, int w, int h, GridCell hover) {
        Theme.hints(g);
        CityMap map = sc.getMap();
        int rows = map.getRows(), cols = map.getCols();
        cell = Math.min((w - 16.0) / cols, (h - 16.0) / rows);
        ox = (w - cell * cols) / 2;
        oy = (h - cell * rows) / 2;

        // ---- terrain (switch on enum)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellType t = map.getType(r, c);
                double x = ox + c * cell, y = oy + r * cell;
                Rectangle2D.Double rect = new Rectangle2D.Double(x, y, cell, cell);
                switch (t) {
                    case ROAD:       g.setColor(new Color(0x1F2F45)); g.fill(rect); break;
                    case OPEN:       g.setColor(new Color(0x16382F)); g.fill(rect); break;
                    case BUILDING:
                        g.setColor(new Color(0x0C1320)); g.fill(rect);
                        g.setColor(new Color(255, 255, 255, 12));
                        g.draw(new Rectangle2D.Double(x + 2, y + 2, cell - 4, cell - 4));
                        break;
                    case RESTRICTED:
                        g.setColor(Theme.alpha(Palette.AMBER, 55)); g.fill(rect);
                        g.setColor(Theme.alpha(Palette.AMBER, 200));
                        g.setStroke(new BasicStroke(1.4f));
                        g.draw(new Line2D.Double(x + 3, y + 3, x + cell - 3, y + cell - 3));
                        g.draw(new Line2D.Double(x + cell - 3, y + 3, x + 3, y + cell - 3));
                        break;
                    default:         g.setColor(new Color(0x1F2F45)); g.fill(rect); break;
                }
            }
        }
        g.setColor(Palette.GRID);
        g.setStroke(new BasicStroke(0.6f));
        for (int r = 0; r <= rows; r++) g.draw(new Line2D.Double(ox, oy + r * cell, ox + cols * cell, oy + r * cell));
        for (int c = 0; c <= cols; c++) g.draw(new Line2D.Double(ox + c * cell, oy, ox + c * cell, oy + rows * cell));

        if (hover != null) {
            g.setColor(Theme.alpha(Palette.TEAL, 60));
            g.fill(new Rectangle2D.Double(ox + hover.getCol() * cell, oy + hover.getRow() * cell, cell, cell));
        }

        drawRoutes(g, sc);
        Icons.base(g, cx(map.getBase().getCol()), cy(map.getBase().getRow()), cell * 0.62, Palette.TEAL);

        // ---- UNIT 4/5: array of parent type, instanceof + downcast to the real subclass
        MapObject[] objects = sc.allObjects();
        for (MapObject o : objects) {
            if (o instanceof SurveyZone) {
                drawZone(g, (SurveyZone) o);
            }
        }
        for (MapObject o : objects) {
            if (o instanceof SightingReport) {
                SightingReport r = (SightingReport) o;
                Icons.report(g, cx(r.getCell().getCol()), cy(r.getCell().getRow()), cell,
                        Palette.clusterColor(r.getClusterIndex()), r.getConfidence());
            }
        }
        int n = sc.getDrones().size();
        for (MapObject o : objects) {
            if (o instanceof Drone) {
                drawDrone(g, (Drone) o, n);
            }
        }
    }

    private void drawRoutes(Graphics2D g, Scenario sc) {
        Plan plan = sc.getActivePlan();
        if (plan == null) return;
        float[] dash = {7f, 5f};
        for (DroneRoute r : plan.getRoutes()) {
            if (r.isEmpty()) continue;
            Color col = Palette.droneColor(r.getDrone().getId());
            double off = (r.getDrone().getId() - 1.5) * 1.6;           // small offset so overlaps stay visible
            Path2D path = new Path2D.Double();
            boolean first = true;
            for (Segment s : r.getSegments()) {
                for (GridCell c : s.getPath()) {
                    double x = cx(c.getCol()) + off, y = cy(c.getRow()) + off;
                    if (first) { path.moveTo(x, y); first = false; } else path.lineTo(x, y);
                }
            }
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f));
            g.setColor(Theme.alpha(col, 200));
            g.draw(path);
        }
    }

    private void drawZone(Graphics2D g, SurveyZone z) {
        double rad = Math.max(2, z.getRadiusCells()) * cell;
        double x = cx(z.getCell().getCol()), y = cy(z.getCell().getRow());
        Color c = Palette.clusterColor(z.getId());
        g.setColor(Theme.alpha(c, 28));
        g.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
        g.setColor(Theme.alpha(c, 150));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
        g.draw(new Line2D.Double(x - 5, y, x + 5, y));              // centroid cross
        g.draw(new Line2D.Double(x, y - 5, x, y + 5));
        g.setFont(Fonts.LABEL);
        g.setColor(c);
        g.drawString(z.getLabel().replace("Zone ", "Z"), (float) (x + 6), (float) (y - 6));
    }

    private void drawDrone(Graphics2D g, Drone d, int total) {
        double row = d.getRow(), col = d.getCol();
        double shift = 0;
        if (d.getStatus() == DroneStatus.IDLE || d.getStatus() == DroneStatus.COMPLETED) {
            shift = (d.getId() - (total - 1) / 2.0) * cell * 0.55;   // park side by side on the pad
        }
        double x = cx(col) + shift, y = cy(row);
        Color c = Palette.droneColor(d.getId());
        if (d.getStatus() == DroneStatus.SURVEYING) {
            g.setColor(Theme.alpha(c, 70));
            g.fill(new Ellipse2D.Double(x - cell * 0.9, y - cell * 0.9, cell * 1.8, cell * 1.8));
        }
        Icons.drone(g, x, y, cell * 0.75, c);
        g.setFont(Fonts.SMALL);
        g.setColor(TEXT);
        g.drawString(String.valueOf(d.getId() + 1), (float) (x - 3), (float) (y - cell * 0.75));
    }
}
