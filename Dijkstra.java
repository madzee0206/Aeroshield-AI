package pathfinding;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import map.CityMap;
import model.GridCell;

/** Manual Dijkstra on the grid (FR-05). Four-neighbour moves, non-negative costs. */
public final class Dijkstra {
    private Dijkstra() { }

    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /** Queue entry. Comparable so PriorityQueue pops the cheapest first. */
    private static final class Node implements Comparable<Node> {
        final GridCell cell;
        final double cost;
        Node(GridCell cell, double cost) { this.cell = cell; this.cost = cost; }
        @Override public int compareTo(Node o) { return Double.compare(cost, o.cost); }
    }

    public static List<GridCell> shortestPath(CityMap map, GridCell start, GridCell goal) throws NoPathException {
        if (!map.isPassable(start) || !map.isPassable(goal)) {
            throw new NoPathException("Start or goal is blocked: " + start + " -> " + goal);
        }
        int rows = map.getRows(), cols = map.getCols();
        double[][] dist = new double[rows][cols];
        for (double[] row : dist) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        GridCell[][] prev = new GridCell[rows][cols];
        PriorityQueue<Node> pq = new PriorityQueue<>();
        dist[start.getRow()][start.getCol()] = 0;
        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            int r = cur.cell.getRow(), c = cur.cell.getCol();
            if (cur.cost > dist[r][c]) {
                continue;                                // stale entry
            }
            if (cur.cell.equals(goal)) {
                break;                                   // cheapest route to goal found
            }
            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (!map.isPassable(nr, nc)) {
                    continue;                            // blocked or outside map
                }
                double nd = cur.cost + map.moveCost(nr, nc);
                if (nd < dist[nr][nc]) {
                    dist[nr][nc] = nd;
                    prev[nr][nc] = cur.cell;
                    pq.add(new Node(new GridCell(nr, nc), nd));
                }
            }
        }
        if (Double.isInfinite(dist[goal.getRow()][goal.getCol()])) {
            throw new NoPathException(start, goal);
        }
        LinkedList<GridCell> path = new LinkedList<>();
        for (GridCell at = goal; at != null; at = prev[at.getRow()][at.getCol()]) {
            path.addFirst(at);
        }
        return path;
    }
}
