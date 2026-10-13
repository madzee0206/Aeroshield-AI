package map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.CellType;
import model.GridCell;

/**
 * Fictional city grid (FR-01). UNIT 5: 2-D arrays.
 * terrain[r][c]    -> permanent type (ROAD / OPEN / BUILDING)
 * restricted[r][c] -> user-toggled no-fly cells
 */
public class CityMap {
    public static final double CELL_METERS = 50.0;

    private final int rows;
    private final int cols;
    private final CellType[][] terrain;
    private final boolean[][] restricted;
    private final GridCell base;

    public CityMap(int rows, int cols) {
        if (rows < 5 || cols < 5) {
            throw new IllegalArgumentException("map must be at least 5x5");
        }
        this.rows = rows;
        this.cols = cols;
        this.terrain = new CellType[rows][cols];
        this.restricted = new boolean[rows][cols];
        for (CellType[] line : terrain) {
            java.util.Arrays.fill(line, CellType.OPEN);
        }
        this.base = new GridCell(rows / 2, cols / 2);
    }

    /** Deterministic city: roads every 6 cells, buildings inside blocks, some parks, a few no-fly cells. */
    public static CityMap generate(long seed, int rows, int cols) {
        CityMap m = new CityMap(rows, cols);
        Random rnd = new Random(seed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r % 6 == 0 || c % 6 == 0) {
                    m.terrain[r][c] = CellType.ROAD;
                } else if (rnd.nextDouble() < 0.12) {
                    m.terrain[r][c] = CellType.OPEN;       // park
                } else {
                    m.terrain[r][c] = CellType.BUILDING;
                }
            }
        }
        m.terrain[m.base.getRow()][m.base.getCol()] = CellType.ROAD;
        int placed = 0, attempts = 0;
        while (placed < 6 && attempts < 500) {
            attempts++;
            int r = rnd.nextInt(rows), c = rnd.nextInt(cols);
            GridCell g = new GridCell(r, c);
            if (m.terrain[r][c] != CellType.ROAD || g.manhattan(m.base) < 5 || m.restricted[r][c]) {
                continue;
            }
            m.restricted[r][c] = true;
            placed++;
        }
        return m;
    }

    public CityMap copy() {                             // snapshot for background planning thread
        CityMap c = new CityMap(rows, cols);
        for (int r = 0; r < rows; r++) {
            System.arraycopy(terrain[r], 0, c.terrain[r], 0, cols);
            System.arraycopy(restricted[r], 0, c.restricted[r], 0, cols);
        }
        return c;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public GridCell getBase() { return base; }
    public boolean inBounds(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }

    public CellType getType(int r, int c) {
        if (r == base.getRow() && c == base.getCol()) return CellType.BASE;
        if (restricted[r][c]) return CellType.RESTRICTED;
        return terrain[r][c];
    }

    public boolean isPassable(int r, int c) { return inBounds(r, c) && getType(r, c).isPassable(); }
    public boolean isPassable(GridCell g) { return isPassable(g.getRow(), g.getCol()); }
    public double moveCost(int r, int c) { return getType(r, c).getMoveCost(); }

    /** Toggle a no-fly cell. Only roads/open cells may be toggled. Returns the new state. */
    public boolean toggleRestricted(GridCell g) {
        if (!inBounds(g.getRow(), g.getCol()) || terrain[g.getRow()][g.getCol()] == CellType.BUILDING
                || g.equals(base)) {
            throw new IllegalArgumentException("Only road or open cells can be restricted");
        }
        boolean now = !restricted[g.getRow()][g.getCol()];
        restricted[g.getRow()][g.getCol()] = now;
        return now;
    }

    public void setRestricted(GridCell g, boolean value) { restricted[g.getRow()][g.getCol()] = value; }
    public void clearRestricted() { for (boolean[] line : restricted) java.util.Arrays.fill(line, false); }

    public List<GridCell> getRestrictedCells() {
        List<GridCell> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (restricted[r][c]) out.add(new GridCell(r, c));
            }
        }
        return out;
    }

    /** Closest passable cell to target (ring search). */
    public GridCell nearestPassable(GridCell target) {
        int tr = Math.max(0, Math.min(rows - 1, target.getRow()));
        int tc = Math.max(0, Math.min(cols - 1, target.getCol()));
        for (int radius = 0; radius < Math.max(rows, cols); radius++) {
            GridCell best = null;
            for (int r = tr - radius; r <= tr + radius; r++) {
                for (int c = tc - radius; c <= tc + radius; c++) {
                    if (!inBounds(r, c) || !isPassable(r, c)) {
                        continue;
                    }
                    GridCell g = new GridCell(r, c);
                    if (best == null || g.manhattan(target) < best.manhattan(target)) {
                        best = g;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return base;
    }
}
