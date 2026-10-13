package physics;

/** UNIT 5: second CHECKED exception, thrown when the battery budget is not enough. */
public class InfeasibleMissionException extends Exception {
    private final double shortfallJoules;

    public InfeasibleMissionException(String msg, double shortfallJoules) {
        super(msg);
        this.shortfallJoules = shortfallJoules;
    }

    public double getShortfallJoules() { return shortfallJoules; }
}
