package pathfinding;

import model.GridCell;

/** UNIT 5: user-defined CHECKED exception (extends Exception) - callers must catch or declare it. */
public class NoPathException extends Exception {
    public NoPathException(GridCell from, GridCell to) {
        super("No valid route from " + from + " to " + to);
    }

    public NoPathException(String message) {
        super(message);
    }
}
