package model;

/** A synthetic incident report (FR-02): coordinates, timestamp and confidence. */
public class SightingReport extends MapObject implements Describable {
    private final double confidence;          // 0..1
    private final int minutesAfterIncident;
    private int clusterIndex = -1;            // filled by K-Means

    public SightingReport(int id, GridCell cell, double confidence, int minutesAfterIncident) {
        super(id, cell);                       // UNIT 4: super(...) constructor call
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be in [0,1]");   // unchecked exception
        }
        this.confidence = confidence;
        this.minutesAfterIncident = minutesAfterIncident;
    }

    public SightingReport(int id, GridCell cell) {
        this(id, cell, 0.8, 0);                // UNIT 3: constructor invocation with this(...)
    }

    public double getConfidence() { return confidence; }
    public int getClusterIndex() { return clusterIndex; }
    public void setClusterIndex(int c) { this.clusterIndex = c; }

    /** Incident assumed at 14:00. */
    public String getTimestamp() {
        int t = 14 * 60 + minutesAfterIncident;
        return String.format("%02d:%02d", (t / 60) % 24, t % 60);
    }

    @Override public String getLabel() { return "Report #" + getId(); }

    @Override
    public String describe() {
        return getLabel() + " at " + cell + " conf=" + Math.round(confidence * 100) + "% t=" + getTimestamp();
    }

    public int getMinutes() { return minutesAfterIncident; }
}
