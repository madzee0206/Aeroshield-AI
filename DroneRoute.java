package planning;

import java.util.ArrayList;
import java.util.List;
import model.Drone;
import model.Segment;
import model.SurveyZone;

/** A drone together with the ordered zones it must survey and the resulting flight segments. */
public class DroneRoute {
    private final Drone drone;
    private final List<SurveyZone> zones = new ArrayList<>();
    private List<Segment> segments = new ArrayList<>();
    private double totalCells, totalSeconds, totalJoules;

    public DroneRoute(Drone drone) { this.drone = drone; }

    public Drone getDrone() { return drone; }
    public List<SurveyZone> getZones() { return zones; }
    public void addZone(SurveyZone z) { zones.add(z); }
    public boolean isEmpty() { return zones.isEmpty(); }
    public List<Segment> getSegments() { return segments; }
    public double getTotalCells() { return totalCells; }
    public double getTotalSeconds() { return totalSeconds; }
    public double getTotalJoules() { return totalJoules; }

    void finish(List<Segment> segments, double cells, double seconds, double joules) {   // package-private
        this.segments = segments;
        this.totalCells = cells;
        this.totalSeconds = seconds;
        this.totalJoules = joules;
    }

    public String zoneNames() {
        if (zones.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zones.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(zones.get(i).getLabel().replace("Zone ", ""));
        }
        return sb.toString();
    }
}
