package analytics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import map.CityMap;
import model.Drone;
import model.GridCell;
import model.Segment;
import model.SurveyZone;
import planning.DroneRoute;
import planning.Plan;
import physics.FlightModel;

/** FR-08 metric calculations. UNIT 5: Collections (HashMap, HashSet). */
public final class MetricsCalculator {
    private MetricsCalculator() { }

    /** Immutable value object holding the metrics of one plan. */
    public static final class Metrics {
        private final double distanceKm, completionMin, energyWh, coveragePct, overlapPct, balanceMin;
        private final int infeasibleZones;

        Metrics(double distanceKm, double completionMin, double energyWh, double coveragePct,
                double overlapPct, double balanceMin, int infeasibleZones) {
            this.distanceKm = distanceKm;
            this.completionMin = completionMin;
            this.energyWh = energyWh;
            this.coveragePct = coveragePct;
            this.overlapPct = overlapPct;
            this.balanceMin = balanceMin;
            this.infeasibleZones = infeasibleZones;
        }

        public double getDistanceKm() { return distanceKm; }
        public double getCompletionMin() { return completionMin; }
        public double getEnergyWh() { return energyWh; }
        public double getCoveragePct() { return coveragePct; }
        public double getOverlapPct() { return overlapPct; }
        public double getBalanceMin() { return balanceMin; }
        public int getInfeasibleZones() { return infeasibleZones; }
    }

    public static Metrics forPlan(Plan plan, int totalReports, FlightModel model, GridCell base) {
        double cells = 0, joules = 0, maxSec = 0;
        int covered = 0, active = 0;
        double sumSec = 0, sumSq = 0;
        Map<GridCell, Integer> visits = new HashMap<>();
        for (DroneRoute r : plan.getRoutes()) {
            if (r.isEmpty()) {
                continue;
            }
            active++;
            cells += r.getTotalCells();
            joules += r.getTotalJoules();
            maxSec = Math.max(maxSec, r.getTotalSeconds());
            sumSec += r.getTotalSeconds();
            sumSq += r.getTotalSeconds() * r.getTotalSeconds();
            for (SurveyZone z : r.getZones()) {
                covered += z.getReportCount();
            }
            Set<GridCell> own = new HashSet<>();
            for (Segment s : r.getSegments()) {
                own.addAll(s.getPath());
            }
            own.remove(base);                        // everyone leaves from the base - not real overlap
            for (GridCell c : own) {
                visits.merge(c, 1, Integer::sum);
            }
        }
        int shared = 0;
        for (int v : visits.values()) {
            if (v > 1) shared++;
        }
        double overlap = visits.isEmpty() ? 0 : 100.0 * shared / visits.size();
        double mean = active == 0 ? 0 : sumSec / active;
        double std = active == 0 ? 0 : Math.sqrt(Math.max(0, sumSq / active - mean * mean));
        double coverage = totalReports == 0 ? 0 : 100.0 * covered / totalReports;
        return new Metrics(model.cellsToMeters(cells) / 1000.0, maxSec / 60.0, joules / 3600.0,
                coverage, overlap, std / 60.0, plan.getUnassigned().size());
    }

    /** Actual distance flown so far in the simulation, in km. */
    public static double travelledKm(List<Drone> drones, FlightModel model) {
        double cells = 0;
        for (Drone d : drones) {
            cells += d.getDistanceCells();
        }
        return model.cellsToMeters(cells) / 1000.0;
    }

    public static double usedBatteryWh(List<Drone> drones) {
        double j = 0;
        for (Drone d : drones) {
            j += d.getInitialEnergyJoules() - d.getEnergyJoules();
        }
        return j / 3600.0;
    }
}
