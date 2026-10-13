package planning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import map.CityMap;
import model.SurveyZone;
import physics.FlightModel;

/**
 * Greedy cost-based assignment (proposed strategy).
 * 1. Heaviest zones first.  2. Each zone goes to the FEASIBLE drone with the lowest cost, where
 *    cost = projected finish time (distance + existing workload) + battery-use penalty.
 */
public class GreedyAssignment extends ZoneAssignment {
    private static final double W_TIME = 1.0;
    private static final double W_BATTERY = 3.0;

    public GreedyAssignment(CityMap map, FlightModel model, double reservePercent) {
        super(map, model, reservePercent);       // UNIT 4: super constructor
    }

    @Override public String getName() { return "Greedy cost-based"; }

    @Override
    protected List<SurveyZone> order(List<SurveyZone> zones) {
        List<SurveyZone> sorted = new ArrayList<>(zones);
        sorted.sort(Comparator.comparingDouble(SurveyZone::getWorkloadSeconds).reversed()
                .thenComparingInt(SurveyZone::getId));
        return sorted;
    }

    @Override
    protected DroneRoute choose(SurveyZone zone, List<DroneRoute> routes) {
        DroneRoute best = null;
        double bestCost = Double.MAX_VALUE;
        for (DroneRoute r : routes) {
            if (!isFeasible(r, zone)) {
                continue;                                    // battery check first
            }
            double[] p = projected(r, zone);
            double batteryUse = 100.0 * p[2] / r.getDrone().getCapacityJoules();
            double cost = W_TIME * p[1] + W_BATTERY * batteryUse;
            if (cost < bestCost - 1e-9) {
                bestCost = cost;
                best = r;
            }
        }
        return best;
    }
}
