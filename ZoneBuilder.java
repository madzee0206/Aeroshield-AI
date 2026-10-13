package planning;

import java.util.ArrayList;
import java.util.List;
import map.CityMap;
import ml.KMeans;
import model.GridCell;
import model.SightingReport;
import model.SurveyZone;

/** Turns K-Means centroids into survey zones (FR-03). A zone is a search area, not proof of a suspect's location. */
public final class ZoneBuilder {
    private ZoneBuilder() { }

    public static List<SurveyZone> build(List<SightingReport> reports, KMeans km, CityMap map) {
        double[][] centroids = km.getCentroids();
        int[] labels = km.getLabels();
        List<SurveyZone> zones = new ArrayList<>();
        for (SightingReport r : reports) {
            r.setClusterIndex(-1);
        }
        for (int j = 0; j < centroids.length; j++) {
            GridCell target = new GridCell((int) Math.round(centroids[j][1]), (int) Math.round(centroids[j][0]));
            GridCell centre = map.nearestPassable(target);
            int count = 0;
            double confSum = 0, radius = 2;
            for (int i = 0; i < reports.size(); i++) {
                if (labels[i] != j) {
                    continue;
                }
                SightingReport r = reports.get(i);
                r.setClusterIndex(j);
                count++;
                confSum += r.getConfidence();
                int dr = Math.abs(r.getCell().getRow() - centre.getRow());
                int dc = Math.abs(r.getCell().getCol() - centre.getCol());
                radius = Math.max(radius, Math.max(dr, dc));
            }
            if (count == 0) {
                continue;
            }
            double workload = 30 + 25 * confSum + 8 * radius;          // seconds of survey time
            zones.add(new SurveyZone(j, centre, count, radius, workload));
        }
        return zones;
    }
}
