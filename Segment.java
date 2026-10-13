package model;

import java.util.List;

/** One leg of a mission: fly along 'path', then survey 'zone' (null for the final return leg). */
public final class Segment {
    private final List<GridCell> path;
    private final SurveyZone zone;
    private final boolean returning;

    public Segment(List<GridCell> path, SurveyZone zone, boolean returning) {
        this.path = path;
        this.zone = zone;
        this.returning = returning;
    }

    public List<GridCell> getPath() { return path; }
    public SurveyZone getZone() { return zone; }
    public boolean isReturning() { return returning; }
}
