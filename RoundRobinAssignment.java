package planning;

import java.util.List;
import map.CityMap;
import model.SurveyZone;
import physics.FlightModel;

/** Simple baseline: zones are dealt to drones in turn, ignoring workload and distance. */
public class RoundRobinAssignment extends ZoneAssignment {
    private int next = 0;

    public RoundRobinAssignment(CityMap map, FlightModel model, double reservePercent) {
        super(map, model, reservePercent);
    }

    @Override public String getName() { return "Round-robin baseline"; }

    @Override
    protected DroneRoute choose(SurveyZone zone, List<DroneRoute> routes) {
        DroneRoute r = routes.get(next % routes.size());
        next++;
        return isFeasible(r, zone) ? r : null;       // flagged when infeasible - no second chance
    }
}
