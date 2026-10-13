package planning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import map.CityMap;
import model.Drone;
import model.GridCell;
import model.Segment;
import model.SurveyZone;
import pathfinding.Dijkstra;
import pathfinding.NoPathException;
import physics.FlightModel;
import physics.InfeasibleMissionException;

/**
 * Abstract strategy for FR-04 (UNIT 4: abstract class + final template method).
 * assign() is the same for every strategy; subclasses only decide choose().
 */
public abstract class ZoneAssignment {
    protected final CityMap map;
    protected final FlightModel model;
    protected final double reserveFraction;
    private final Map<String, List<GridCell>> pathCache = new HashMap<>();

    protected ZoneAssignment(CityMap map, FlightModel model, double reservePercent) {
        this.map = map;
        this.model = model;
        this.reserveFraction = reservePercent / 100.0;
    }

    public abstract String getName();

    /** Pick a route for the zone, or return null if no drone can take it. */
    protected abstract DroneRoute choose(SurveyZone zone, List<DroneRoute> routes);

    /** Hook: subclasses may reorder the zones (default = given order). */
    protected List<SurveyZone> order(List<SurveyZone> zones) { return new ArrayList<>(zones); }

    public final Plan assign(List<Drone> drones, List<SurveyZone> zones) {
        Plan plan = new Plan(getName());
        for (Drone d : drones) {
            plan.getRoutes().add(new DroneRoute(d));
        }
        for (SurveyZone z : order(zones)) {
            if (pathBetween(map.getBase(), z.getCell()) == null) {
                plan.flag(z, "unreachable from base (blocked by buildings / restricted cells)");
                continue;
            }
            DroneRoute r = choose(z, plan.getRoutes());
            if (r == null) {
                plan.flag(z, "rejected - no drone has enough battery (incl. return reserve)");
            } else {
                r.addZone(z);
            }
        }
        for (DroneRoute r : plan.getRoutes()) {
            buildSegments(r);
        }
        return plan;
    }

    /** Cached Dijkstra. Returns null when no valid route exists. */
    protected final List<GridCell> pathBetween(GridCell a, GridCell b) {
        String key = a + ">" + b;
        if (pathCache.containsKey(key)) {
            return pathCache.get(key);
        }
        List<GridCell> path;
        try {
            path = Dijkstra.shortestPath(map, a, b);
        } catch (NoPathException e) {          // UNIT 5: checked exception handled
            path = null;
        }
        pathCache.put(key, path);
        return path;
    }

    /** {cells, seconds, joules} for route + optional extra zone, or null if some leg is unreachable. */
    protected final double[] projected(DroneRoute r, SurveyZone extra) {
        List<SurveyZone> stops = new ArrayList<>(r.getZones());
        if (extra != null) stops.add(extra);
        GridCell cur = map.getBase();
        double cells = 0, survey = 0;
        for (SurveyZone z : stops) {
            List<GridCell> p = pathBetween(cur, z.getCell());
            if (p == null) return null;
            cells += p.size() - 1;
            survey += z.getWorkloadSeconds();
            cur = z.getCell();
        }
        List<GridCell> back = pathBetween(cur, map.getBase());
        if (back == null) return null;
        cells += back.size() - 1;
        double seconds = model.flightTime(model.cellsToMeters(cells)) + survey;
        return new double[]{cells, seconds, model.requiredEnergy(cells, survey)};
    }

    /** Battery feasibility: required energy + return reserve must fit in what the drone holds. */
    protected final boolean isFeasible(DroneRoute r, SurveyZone z) {
        double[] p = projected(r, z);
        if (p == null) return false;
        Drone d = r.getDrone();
        try {
            model.checkMission(d.getInitialEnergyJoules(), p[2], reserveFraction * d.getCapacityJoules());
            return true;
        } catch (InfeasibleMissionException e) {
            return false;
        }
    }

    private void buildSegments(DroneRoute r) {
        List<Segment> segs = new ArrayList<>();
        GridCell cur = map.getBase();
        for (SurveyZone z : r.getZones()) {
            segs.add(new Segment(pathBetween(cur, z.getCell()), z, false));
            cur = z.getCell();
        }
        if (!r.isEmpty()) {
            segs.add(new Segment(pathBetween(cur, map.getBase()), null, true));
            double[] p = projected(r, null);
            r.finish(segs, p[0], p[1], p[2]);
        }
    }
}
