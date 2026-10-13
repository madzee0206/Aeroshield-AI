package model;

/** Immutable grid coordinate. UNIT 4: overrides equals/hashCode/toString from Object. */
public final class GridCell {
    private final int row;
    private final int col;

    public GridCell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public int manhattan(GridCell o) {
        return Math.abs(row - o.row) + Math.abs(col - o.col);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GridCell)) return false;
        GridCell o = (GridCell) obj;                    // typecasting (downcast)
        return row == o.row && col == o.col;
    }

    @Override
    public int hashCode() { return 31 * row + col; }

    @Override
    public String toString() { return "(" + row + "," + col + ")"; }
}
