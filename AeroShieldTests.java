package tests;

import analytics.MetricsCalculator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import map.CityMap;
import ml.KMeans;
import model.CellType;
import model.Drone;
import model.DroneStatus;
import model.GridCell;
import pathfinding.Dijkstra;
import pathfinding.NoPathException;
import physics.FlightModel;
import physics.InfeasibleMissionException;
import planning.DroneRoute;
import planning.Plan;
import simulation.Scenario;
import simulation.SimulationEngine;

/**
 * Console test-runner for the Testing Requirements (section 7). No JUnit needed.
 * Run:  java -cp out tests.AeroShieldTests
 */
public class AeroShieldTests {
    private static int passed = 0, failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
    }

    public static void main(String[] args) throws Exception {
        emptyScenario();
        addReport();
        kmeans();
        blockedRoute();
        zoneAssignmentAndBattery();
        lowBattery();
        pauseResumeReset();
        analyticsMatch();
        reproducible();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void emptyScenario() {
        Scenario sc = new Scenario(42);
        sc.clearReports();
        try {
            sc.computePlans();
            check("Empty scenario: loads and plans without errors", sc.getZones().isEmpty());
        } catch (RuntimeException e) {
            check("Empty scenario: loads and plans without errors (" + e + ")", false);
        }
    }

    static void addReport() {
        Scenario sc = new Scenario(42);
        sc.clearReports();
        GridCell road = new GridCell(0, 0);
        boolean added = sc.toggleReport(road);
        check("Add report: appears at selected cell", added && sc.getReports().size() == 1
                && sc.getReports().get(0).getCell().equals(road));
        sc.toggleReport(road);
        check("Remove report: second click removes it", sc.getReports().isEmpty());
        GridCell building = null;
        for (int r = 0; r < Scenario.ROWS && building == null; r++)
            for (int c = 0; c < Scenario.COLS; c++)
                if (sc.getMap().getType(r, c) == CellType.BUILDING) { building = new GridCell(r, c); break; }
        try {
            sc.toggleReport(building);
            check("Add report on building is rejected", false);
        } catch (IllegalArgumentException e) {
            check("Add report on building is rejected", true);
        }
    }

    static void kmeans() {
        double[][] pts = new double[20][2];
        for (int i = 0; i < 20; i++) { pts[i][0] = (i % 4) * 10 + (i % 3); pts[i][1] = (i / 4) * 2; }
        KMeans km = new KMeans(4, 1);
        km.fit(pts);
        Set<Integer> used = new HashSet<>();
        for (int l : km.getLabels()) used.add(l);
        check("K-Means: 20 points grouped into 4 clusters", km.getCentroids().length == 4 && used.size() == 4);
        KMeans small = new KMeans(8, 1);
        small.fit(new double[][]{{1, 1}, {5, 5}});
        check("K-Means: k larger than n is reduced to n", small.getCentroids().length == 2);
    }

    static void blockedRoute() throws Exception {
        CityMap m = new CityMap(9, 9);                       // all open
        GridCell a = new GridCell(4, 0), b = new GridCell(4, 8);
        int straight = Dijkstra.shortestPath(m, a, b).size();
        for (int r = 0; r < 9; r++) m.setRestricted(new GridCell(r, 2), true);   // full wall
        try {
            Dijkstra.shortestPath(m, a, b);
            check("Blocked route: full wall -> NoPathException", false);
        } catch (NoPathException e) {
            check("Blocked route: full wall -> NoPathException", true);
        }
        m.setRestricted(new GridCell(0, 2), false);              // gap at the top
        List<GridCell> detour = Dijkstra.shortestPath(m, a, b);
        boolean avoids = true;
        for (GridCell g : detour) if (!m.isPassable(g)) avoids = false;
        check("Blocked route: alternate route through the gap, longer than straight", avoids && detour.size() > straight);
        check("Dijkstra: minimum cost on open grid (Manhattan distance)", straight - 1 == a.manhattan(b));
    }

    static void zoneAssignmentAndBattery() {
        Scenario sc = new Scenario(42);
        sc.computePlans();
        Plan p = sc.getOptimizedPlan();
        boolean feasible = true;
        for (DroneRoute r : p.getRoutes()) {
            if (r.isEmpty()) continue;
            double reserve = sc.getReservePercent() / 100.0 * r.getDrone().getCapacityJoules();
            if (r.getTotalJoules() + reserve > r.getDrone().getInitialEnergyJoules() + 1e-6) feasible = false;
        }
        check("Zone assignment: every assigned route fits battery + reserve", feasible);
        int assigned = 0;
        for (DroneRoute r : p.getRoutes()) assigned += r.getZones().size();
        check("Zone assignment: assigned + flagged = all zones", assigned + p.getUnassigned().size() == sc.getZones().size());
    }

    static void lowBattery() {
        Scenario sc = new Scenario(42);
        sc.configure(5, 20, new double[]{12, 10}, false);
        sc.computePlans();
        check("Low battery: infeasible zones are flagged, not assigned",
                !sc.getOptimizedPlan().getUnassigned().isEmpty() && !sc.getOptimizedPlan().getWarnings().isEmpty());
        FlightModel fm = FlightModel.standard();
        try {
            fm.checkMission(1000, 5000, 100);
            check("FlightModel.checkMission throws when battery too small", false);
        } catch (InfeasibleMissionException e) {
            check("FlightModel.checkMission throws when battery too small", e.getShortfallJoules() > 0);
        }
    }

    static void pauseResumeReset() {
        Scenario sc = new Scenario(42);
        sc.computePlans();
        SimulationEngine eng = new SimulationEngine(sc);
        eng.start();
        for (int i = 0; i < 30; i++) eng.tick(0.033);
        Drone d = null;
        for (Drone x : sc.getDrones()) if (x.getStatus() != DroneStatus.IDLE) { d = x; break; }
        eng.pause();
        double r0 = d.getRow(), c0 = d.getCol(), e0 = d.getEnergyJoules();
        for (int i = 0; i < 30; i++) eng.tick(0.033);
        check("Pause: drones do not move or use battery",
                d.getRow() == r0 && d.getCol() == c0 && d.getEnergyJoules() == e0);
        eng.resume();
        for (int i = 0; i < 30; i++) eng.tick(0.033);
        check("Resume: drones move again", d.getRow() != r0 || d.getCol() != c0);
        eng.reset();
        boolean ok = eng.getState() == SimulationEngine.State.READY && eng.getSimTime() == 0 && eng.getDispatched() == 0;
        for (Drone x : sc.getDrones())
            ok &= x.getStatus() == DroneStatus.IDLE && x.getDistanceCells() == 0
                    && Math.abs(x.getEnergyJoules() - x.getInitialEnergyJoules()) < 1e-9
                    && x.getRow() == sc.getMap().getBase().getRow();
        check("Reset: engine, drones, metrics back to initial values", ok);
    }

    static void analyticsMatch() {
        Scenario sc = new Scenario(42);
        sc.computePlans();
        SimulationEngine eng = new SimulationEngine(sc);
        eng.start();
        int guard = 0;
        while (eng.getState() == SimulationEngine.State.RUNNING && guard++ < 200000) eng.tick(0.033);
        check("Simulation finishes", eng.getState() == SimulationEngine.State.FINISHED);
        MetricsCalculator.Metrics m = MetricsCalculator.forPlan(sc.getActivePlan(), sc.getReports().size(),
                sc.getModel(), sc.getMap().getBase());
        double actual = MetricsCalculator.travelledKm(sc.getDrones(), sc.getModel());
        check(String.format("Analytics: planned %.3f km == simulated %.3f km", m.getDistanceKm(), actual),
                Math.abs(m.getDistanceKm() - actual) < 1e-6);
        boolean reserveKept = true;
        for (Drone d : sc.getDrones())
            if (d.getDistanceCells() > 0 && d.getBatteryPercent() < sc.getReservePercent() - 1e-6) reserveKept = false;
        check("Safety: every drone lands with at least the reserve left", reserveKept);
        double usedWh = MetricsCalculator.usedBatteryWh(sc.getDrones());
        check(String.format("Analytics: planned %.2f Wh ~ used %.2f Wh", m.getEnergyWh(), usedWh),
                Math.abs(m.getEnergyWh() - usedWh) < 0.05);
    }

    static void reproducible() {
        Scenario a = new Scenario(7), b = new Scenario(7);
        a.computePlans();
        b.computePlans();
        List<String> sa = new ArrayList<>(), sb = new ArrayList<>();
        for (DroneRoute r : a.getOptimizedPlan().getRoutes()) sa.add(r.zoneNames() + "/" + r.getTotalCells());
        for (DroneRoute r : b.getOptimizedPlan().getRoutes()) sb.add(r.zoneNames() + "/" + r.getTotalCells());
        check("Repeated scenario: same seed gives identical plan " + sa, sa.equals(sb));
    }
}
