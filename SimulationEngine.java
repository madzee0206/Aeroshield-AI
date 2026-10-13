package simulation;

import analytics.MetricsCalculator;
import java.util.ArrayList;
import java.util.List;
import model.Drone;
import model.DroneStatus;
import model.GridCell;
import model.Segment;
import pathfinding.Dijkstra;
import pathfinding.NoPathException;
import physics.FlightModel;
import planning.DroneRoute;
import planning.Plan;

/**
 * Pure simulation logic (no Swing): the UI's javax.swing.Timer just calls tick().
 * FR-07: start / pause / resume / reset, drone status machine, event feed.
 */
public class SimulationEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }

    /** 1 real second = TIME_SCALE simulated seconds at 1x speed. */
    public static final double TIME_SCALE = 40.0;

    private final Scenario sc;
    private final FlightModel model;
    private State state = State.READY;
    private double simTime;
    private double speed = 1.0;
    private int dispatched;

    public SimulationEngine(Scenario sc) {
        this.sc = sc;
        this.model = sc.getModel();
    }

    public State getState() { return state; }
    public double getSimTime() { return simTime; }
    public int getDispatched() { return dispatched; }
    public void setSpeed(double s) { speed = Math.max(0.25, Math.min(16, s)); }
    public double getSpeed() { return speed; }
    public boolean isEditable() { return state == State.READY; }

    public void start() {
        Plan plan = sc.getActivePlan();
        if (state != State.READY) {
            sc.getLog().add(simTime, "Press RESET before starting a new run");
            return;
        }
        if (plan == null) {
            sc.getLog().add(simTime, "WARNING no plan yet - wait for planning to finish");
            return;
        }
        for (DroneRoute r : plan.getRoutes()) {
            Drone d = r.getDrone();
            if (r.isEmpty()) {
                sc.getLog().add(simTime, d.getName() + " has no feasible zone - stays on the ground");
                continue;
            }
            d.setSegments(r.getSegments());
            d.setStatus(DroneStatus.FLYING);
            dispatched++;
            sc.getLog().add(simTime, d.getName() + " dispatched to zone(s) " + r.zoneNames()
                    + " (est. " + Math.round(r.getTotalSeconds() / 60) + " min)");
        }
        state = dispatched == 0 ? State.FINISHED : State.RUNNING;
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            sc.getLog().add(simTime, "Simulation paused");
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            sc.getLog().add(simTime, "Simulation resumed");
        }
    }

    public void reset() {
        for (Drone d : sc.getDrones()) {
            d.reset();                                 // Resettable interface
        }
        state = State.READY;
        simTime = 0;
        dispatched = 0;
        sc.getLog().clear();
        sc.getLog().add(0, "Simulation reset - drones back at base with full initial charge");
    }

    /** Advance the simulation by realSeconds of wall-clock time. */
    public void tick(double realSeconds) {
        if (state != State.RUNNING) {
            return;
        }
        double dt = realSeconds * TIME_SCALE * speed;
        simTime += dt;
        boolean allDone = true;
        for (Drone d : sc.getDrones()) {
            stepDrone(d, dt);
            if (!d.isFinished()) {
                allDone = false;
            }
        }
        if (allDone) {
            state = State.FINISHED;
            sc.getLog().add(simTime, String.format("MISSION COMPLETE - %.2f km flown in %.1f min",
                    MetricsCalculator.travelledKm(sc.getDrones(), model), simTime / 60));
        }
    }

    private void stepDrone(Drone d, double dt) {
        int guard = 0;
        while (dt > 1e-9 && !d.isFinished() && d.hasSegment() && guard++ < 10000) {
            Segment seg = d.currentSegment();
            if (d.getStatus() == DroneStatus.SURVEYING) {
                double use = Math.min(dt, d.getSurveyRemaining());
                d.drain(model.hoverEnergy(use));
                d.setSurveyRemaining(d.getSurveyRemaining() - use);
                dt -= use;
                if (d.getSurveyRemaining() <= 1e-9) {
                    sc.getLog().add(simTime, d.getName() + " finished surveying " + seg.getZone().getLabel());
                    d.nextSegment();
                    d.setStatus(d.currentSegment().isReturning() ? DroneStatus.RETURNING : DroneStatus.FLYING);
                }
                continue;
            }
            List<GridCell> path = seg.getPath();
            if (d.getPathIndex() >= path.size() - 1) {
                arrive(d, seg);
                continue;
            }
            double need = (1.0 - d.getProgress()) * model.secondsPerCell();
            double use = Math.min(dt, need);
            double cells = use / model.secondsPerCell();
            d.drain(model.energy(model.getCruisePowerW(), use));
            d.addDistance(cells);
            d.setProgress(d.getProgress() + cells);
            dt -= use;
            if (d.getProgress() >= 1.0 - 1e-9) {
                d.setPathIndex(d.getPathIndex() + 1);
                d.setProgress(0);
            }
            updatePosition(d, path);
            if (d.getStatus() == DroneStatus.FLYING && !batteryStillSafe(d)) {
                abortToBase(d);
            }
        }
    }

    private void updatePosition(Drone d, List<GridCell> path) {
        int i = Math.min(d.getPathIndex(), path.size() - 1);
        GridCell a = path.get(i);
        GridCell b = path.get(Math.min(i + 1, path.size() - 1));
        double p = d.getProgress();
        d.setPosition(a.getRow() + (b.getRow() - a.getRow()) * p, a.getCol() + (b.getCol() - a.getCol()) * p);
    }

    private void arrive(Drone d, Segment seg) {
        if (seg.getZone() != null) {
            d.setStatus(DroneStatus.SURVEYING);
            d.setSurveyRemaining(seg.getZone().getWorkloadSeconds());
            sc.getLog().add(simTime, d.getName() + " arrived at " + seg.getZone().getLabel() + ", surveying");
        } else {
            d.setStatus(DroneStatus.COMPLETED);
            d.nextSegment();
            sc.getLog().add(simTime, String.format("%s landed at base, battery %.0f%%",
                    d.getName(), d.getBatteryPercent()));
        }
    }

    /** Runtime safety net: must always keep enough charge to get home plus half the reserve. */
    private boolean batteryStillSafe(Drone d) {
        GridCell here = new GridCell((int) Math.round(d.getRow()), (int) Math.round(d.getCol()));
        double homeJ = model.flightEnergyForCells(here.manhattan(sc.getMap().getBase()));
        double halfReserve = 0.5 * sc.getReservePercent() / 100.0 * d.getCapacityJoules();
        return d.getEnergyJoules() >= homeJ + halfReserve;
    }

    private void abortToBase(Drone d) {
        List<GridCell> old = d.currentSegment().getPath();
        int i = Math.min(d.getPathIndex(), old.size() - 1);
        GridCell next = old.get(Math.min(i + 1, old.size() - 1));
        try {
            List<GridCell> home = Dijkstra.shortestPath(sc.getMap(), next, sc.getMap().getBase());
            List<GridCell> path = new ArrayList<>();
            path.add(old.get(i));
            path.addAll(home);
            double keep = d.getProgress();
            List<Segment> only = new ArrayList<>();
            only.add(new Segment(path, null, true));
            d.setSegments(only);
            d.setProgress(keep);
            d.setStatus(DroneStatus.RETURNING);
            sc.getLog().add(simTime, "WARNING " + d.getName() + " battery low - mission aborted, returning to base");
        } catch (NoPathException e) {
            sc.getLog().add(simTime, "WARNING " + d.getName() + " cannot find a way home: " + e.getMessage());
        }
    }
}
