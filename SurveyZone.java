package model;

/** Survey zone derived from a K-Means cluster centroid (FR-03). */
public class SurveyZone extends MapObject implements Describable {
    private final int reportCount;
    private final double radiusCells;
    private final double workloadSeconds;     // time the drone must hover/survey

    public SurveyZone(int clusterIndex, GridCell centre, int reportCount, double radiusCells, double workloadSeconds) {
        super(clusterIndex, centre);
        this.reportCount = reportCount;
        this.radiusCells = radiusCells;
        this.workloadSeconds = workloadSeconds;
    }

    public int getReportCount() { return reportCount; }
    public double getRadiusCells() { return radiusCells; }
    public double getWorkloadSeconds() { return workloadSeconds; }

    @Override public String getLabel() { return "Zone " + (char) ('A' + getId()); }

    @Override
    public String describe() {
        return getLabel() + " " + cell + " reports=" + reportCount + " survey=" + Math.round(workloadSeconds) + "s";
    }
}
