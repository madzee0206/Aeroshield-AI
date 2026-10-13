package planning;

import java.util.ArrayList;
import java.util.List;
import model.Drone;
import model.SurveyZone;

/** Result of one assignment strategy: routes, zones that could not be assigned, and warnings. */
public class Plan {
    private final String strategyName;
    private final List<DroneRoute> routes = new ArrayList<>();
    private final List<SurveyZone> unassigned = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public Plan(String strategyName) { this.strategyName = strategyName; }

    public String getStrategyName() { return strategyName; }
    public List<DroneRoute> getRoutes() { return routes; }
    public List<SurveyZone> getUnassigned() { return unassigned; }
    public List<String> getWarnings() { return warnings; }

    public void flag(SurveyZone z, String reason) {
        unassigned.add(z);
        warnings.add(z.getLabel() + ": " + reason);
    }

    public DroneRoute routeFor(Drone d) {
        for (DroneRoute r : routes) {
            if (r.getDrone() == d) return r;
        }
        return null;
    }
}
