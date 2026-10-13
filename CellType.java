package model;

/** UNIT 3/4: enum with fields, constructor and methods. */
public enum CellType {
    ROAD(true, 1.0), OPEN(true, 1.0), BUILDING(false, 0), RESTRICTED(false, 0), BASE(true, 1.0);

    private final boolean passable;
    private final double moveCost;      // equal costs initially (FR-05)

    CellType(boolean passable, double moveCost) {
        this.passable = passable;
        this.moveCost = moveCost;
    }

    public boolean isPassable() { return passable; }
    public double getMoveCost() { return moveCost; }
}
