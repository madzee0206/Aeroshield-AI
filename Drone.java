package model;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIT 4: Drone extends Aircraft extends MapObject (multi-level inheritance), overrides abstract methods.
 * UNIT 5: implements two interfaces (Describable, Resettable) = multiple inheritance through interfaces.
 */
public class Drone extends Aircraft implements Describable, Resettable {
    public static final double CAPACITY_JOULES = 50 * 3600.0;     // 50 Wh pack
    private final GridCell home;
    private DroneStatus status = DroneStatus.IDLE;

    // live state (changed by the simulation engine)
    private double row, col;
    private List<Segment> segments = new ArrayList<>();
    private int segIndex, pathIndex;
    private double progress, surveyRemaining, distanceCells;

    public Drone(int id, GridCell home, double startPercent) {
        super(id, home, CAPACITY_JOULES, startPercent);
        this.home = home;
        resetPosition();
    }

    public Drone(int id, GridCell home) {
        this(id, home, 100.0);
    }

    private void resetPosition() {
        row = home.getRow();
        col = home.getCol();
        cell = home;
    }

    public String getName() { return String.format("DRONE %02d", getId() + 1); }
    @Override public String getLabel() { return getName(); }
    @Override public String getModelName() { return "AS-Quad X1"; }

    public DroneStatus getStatus() { return status; }
    public void setStatus(DroneStatus s) { status = s; }

    public double getRow() { return row; }
    public double getCol() { return col; }
    public void setPosition(double r, double c) { row = r; col = c; }

    public List<Segment> getSegments() { return segments; }
    public void setSegments(List<Segment> s) {
        segments = new ArrayList<>(s);
        segIndex = 0;
        pathIndex = 0;
        progress = 0;
    }
    public boolean hasSegment() { return segIndex < segments.size(); }
    public Segment currentSegment() { return segments.get(segIndex); }
    public void nextSegment() { segIndex++; pathIndex = 0; progress = 0; }

    public int getPathIndex() { return pathIndex; }
    public void setPathIndex(int i) { pathIndex = i; }
    public double getProgress() { return progress; }
    public void setProgress(double p) { progress = p; }
    public double getSurveyRemaining() { return surveyRemaining; }
    public void setSurveyRemaining(double s) { surveyRemaining = s; }
    public double getDistanceCells() { return distanceCells; }
    public void addDistance(double cells) { distanceCells += cells; }

    public boolean isFinished() { return status == DroneStatus.IDLE || status == DroneStatus.COMPLETED; }

    @Override
    public void reset() {                       // from Resettable
        restoreEnergy();
        status = DroneStatus.IDLE;
        segments = new ArrayList<>();
        segIndex = 0; pathIndex = 0; progress = 0; surveyRemaining = 0; distanceCells = 0;
        resetPosition();
    }

    @Override
    public String describe() {                  // from Describable
        return getName() + " [" + status + "] battery " + Math.round(getBatteryPercent()) + "%";
    }
}
