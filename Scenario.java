package simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import map.CityMap;
import ml.KMeans;
import model.Drone;
import model.GridCell;
import model.MapObject;
import model.SightingReport;
import model.SurveyZone;
import physics.FlightModel;
import planning.GreedyAssignment;
import planning.Plan;
import planning.RoundRobinAssignment;
import planning.ZoneBuilder;

/**
 * All scenario data: map, reports, drones, zones and the two plans (optimised + baseline).
 * Planning runs on a background Thread so the Swing UI never freezes (UNIT 6).
 */
public class Scenario {
    public static final int ROWS = 25;
    public static final int COLS = 37;

    private String name = "";
    private long seed;
    private int clusterCount = 5;
    private double reservePercent = 20.0;
    private double[] batteryLevels = {100, 95, 90, 70};
    private boolean useBaseline = false;

    private CityMap map;
    private final List<SightingReport> reports = new ArrayList<>();
    private final List<Drone> drones = new ArrayList<>();
    private List<SurveyZone> zones = new ArrayList<>();
    private volatile Plan optimizedPlan;
    private volatile Plan baselinePlan;
    private final FlightModel model = FlightModel.standard();
    private final EventLog log = new EventLog();
    private int nextReportId = 1;
    private boolean planning;
    private boolean pending;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public Scenario(long seed) {
        newScenario(seed);
    }

    // ---------------------------------------------------------------- scenario set-up
    public synchronized void newScenario(long seed) {
        this.seed = seed;
        this.name = String.format("BANK THEFT CASE #%03d", seed);
        map = CityMap.generate(seed, ROWS, COLS);
        reports.clear();
        nextReportId = 1;
        generateReports(22);
        rebuildDrones();
        zones = new ArrayList<>();
        optimizedPlan = null;
        baselinePlan = null;
        log.clear();
        log.add(0, "New scenario created (seed " + seed + "): " + reports.size() + " synthetic reports");
    }

    /** Hot-spots with Gaussian scatter + a few random noise reports. Fully deterministic per seed. */
    private void generateReports(int count) {
        Random rnd = new Random(seed * 31 + 7);
        List<GridCell> hot = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            hot.add(map.nearestPassable(new GridCell(3 + rnd.nextInt(ROWS - 6), 3 + rnd.nextInt(COLS - 6))));
        }
        int guard = 0;
        while (reports.size() < count && guard++ < 500) {
            GridCell target;
            if (reports.size() < count - 4) {
                GridCell h = hot.get(rnd.nextInt(hot.size()));
                target = new GridCell(h.getRow() + (int) Math.round(rnd.nextGaussian() * 3),
                        h.getCol() + (int) Math.round(rnd.nextGaussian() * 4));
            } else {
                target = new GridCell(rnd.nextInt(ROWS), rnd.nextInt(COLS));
            }
            GridCell cell = map.nearestPassable(target);
            if (cell.equals(map.getBase()) || findReport(cell) != null) {
                continue;
            }
            double conf = 0.4 + 0.55 * rnd.nextDouble();
            reports.add(new SightingReport(nextReportId++, cell, conf, rnd.nextInt(45)));
        }
    }

    private void rebuildDrones() {
        drones.clear();
        for (int i = 0; i < batteryLevels.length; i++) {
            drones.add(new Drone(i, map.getBase(), batteryLevels[i]));
        }
    }

    public synchronized void clearReports() {
        reports.clear();
        zones = new ArrayList<>();
    }

    // ---------------------------------------------------------------- editing (FR-01 / FR-02)
    private SightingReport findReport(GridCell c) {
        for (SightingReport r : reports) {
            if (r.getCell().equals(c)) return r;
        }
        return null;
    }

    /** Adds a report on the cell, or removes it if one is already there. Returns true if added. */
    public synchronized boolean toggleReport(GridCell cell) {
        if (!map.inBounds(cell.getRow(), cell.getCol()) || !map.isPassable(cell)) {
            throw new IllegalArgumentException("Reports can only be placed on road/open cells");
        }
        SightingReport existing = findReport(cell);
        if (existing != null) {
            reports.remove(existing);
            return false;
        }
        reports.add(new SightingReport(nextReportId++, cell));
        return true;
    }

    public synchronized boolean toggleRestricted(GridCell cell) {
        boolean now = map.toggleRestricted(cell);
        // drop reports that would now sit on a blocked cell
        reports.removeIf(r -> !map.isPassable(r.getCell()));
        return now;
    }

    public synchronized void configure(int k, double reservePercent, double[] batteries, boolean baseline) {
        if (k < 1 || k > 8) throw new IllegalArgumentException("clusters must be 1..8");
        if (reservePercent < 0 || reservePercent > 60) throw new IllegalArgumentException("reserve must be 0..60 %");
        if (batteries.length < 1 || batteries.length > 6) throw new IllegalArgumentException("1..6 drones supported");
        this.clusterCount = k;
        this.reservePercent = reservePercent;
        this.useBaseline = baseline;
        this.batteryLevels = batteries.clone();
        rebuildDrones();
    }

    /** Used by ScenarioStore when loading a saved file. */
    public synchronized void restore(long seed, int k, double reserve, double[] batteries,
                                     List<SightingReport> loaded, List<GridCell> restricted) {
        this.seed = seed;
        this.name = String.format("BANK THEFT CASE #%03d", seed);
        map = CityMap.generate(seed, ROWS, COLS);
        map.clearRestricted();
        for (GridCell g : restricted) map.setRestricted(g, true);
        reports.clear();
        reports.addAll(loaded);
        nextReportId = 1;
        for (SightingReport r : reports) nextReportId = Math.max(nextReportId, r.getId() + 1);
        configure(k, reserve, batteries, useBaseline);
        optimizedPlan = null;
        baselinePlan = null;
        log.clear();
        log.add(0, "Scenario loaded from file");
    }

    // ---------------------------------------------------------------- planning (UNIT 6 threads)
    /** Synchronous planning: K-Means -> zones -> greedy plan + baseline plan. */
    public void computePlans() {
        List<SightingReport> rs;
        List<Drone> ds;
        CityMap m;
        int k;
        double reserve;
        long sd;
        synchronized (this) {                     // take a consistent snapshot, then release the lock
            rs = new ArrayList<>(reports);
            ds = new ArrayList<>(drones);
            m = map.copy();
            k = clusterCount;
            reserve = reservePercent;
            sd = seed;
        }
        double[][] pts = new double[rs.size()][2];
        for (int i = 0; i < rs.size(); i++) {
            pts[i][0] = rs.get(i).getCell().getCol();
            pts[i][1] = rs.get(i).getCell().getRow();
        }
        KMeans km = new KMeans(k, sd);
        km.fit(pts);
        List<SurveyZone> z = ZoneBuilder.build(rs, km, m);
        Plan opt = new GreedyAssignment(m, model, reserve).assign(ds, z);
        Plan base = new RoundRobinAssignment(m, model, reserve).assign(ds, z);
        synchronized (this) {
            zones = z;
            optimizedPlan = opt;
            baselinePlan = base;
        }
        log.add(0, "Plan ready: " + rs.size() + " reports -> " + z.size() + " zones, K-Means converged in "
                + km.getIterations() + " iterations");
        for (String w : getActivePlan().getWarnings()) {
            log.add(0, "WARNING " + w);
        }
    }

    /** Runs computePlans() on a worker thread; if called while busy, one more run is queued. */
    public void replanAsync(Runnable onDone) {
        synchronized (this) {
            if (planning) {
                pending = true;
                return;
            }
            planning = true;
        }
        Thread worker = new Thread(() -> {
            boolean again;
            do {
                synchronized (Scenario.this) { pending = false; }
                computePlans();
                synchronized (Scenario.this) { again = pending; }
            } while (again);
            synchronized (Scenario.this) { planning = false; }
            if (onDone != null) onDone.run();
        }, "planner-thread");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized boolean isPlanning() { return planning; }

    // ---------------------------------------------------------------- accessors
    public synchronized List<SightingReport> getReports() { return new ArrayList<>(reports); }
    public List<Drone> getDrones() { return drones; }
    public synchronized List<SurveyZone> getZones() { return new ArrayList<>(zones); }
    public CityMap getMap() { return map; }
    public Plan getOptimizedPlan() { return optimizedPlan; }
    public Plan getBaselinePlan() { return baselinePlan; }
    public Plan getActivePlan() { return useBaseline ? baselinePlan : optimizedPlan; }
    public FlightModel getModel() { return model; }
    public EventLog getLog() { return log; }
    public String getName() { return name; }
    public long getSeed() { return seed; }
    public int getClusterCount() { return clusterCount; }
    public double getReservePercent() { return reservePercent; }
    public double[] getBatteryLevels() { return batteryLevels.clone(); }
    public boolean isUseBaseline() { return useBaseline; }

    /** UNIT 5: array of the parent type holding different subclasses (inheritance in arrays). */
    public synchronized MapObject[] allObjects() {
        MapObject[] all = new MapObject[zones.size() + reports.size() + drones.size()];
        int i = 0;
        for (SurveyZone z : zones) all[i++] = z;
        for (SightingReport r : reports) all[i++] = r;
        for (Drone d : drones) all[i++] = d;
        return all;
    }

    public void addListener(Runnable r) { listeners.add(r); }
    public void fireChanged() { for (Runnable r : listeners) r.run(); }
}
