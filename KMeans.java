package ml;

import java.util.Arrays;
import java.util.Random;

/**
 * Manually implemented K-Means (FR-03). UNIT 5: 1-D and 2-D arrays.
 * points[i] = {x, y}; centroids[j] = {x, y}; labels[i] = cluster of point i.
 * Seeded Random -> reproducible results.
 */
public class KMeans {
    private final int k;
    private final int maxIterations;
    private final long seed;
    private double[][] centroids = new double[0][];
    private int[] labels = new int[0];
    private int iterations;

    public KMeans(int k, long seed) {
        this(k, 100, seed);
    }

    public KMeans(int k, int maxIterations, long seed) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be >= 1");
        }
        this.k = k;
        this.maxIterations = maxIterations;
        this.seed = seed;
    }

    public void fit(double[][] points) {
        int n = points.length;
        if (n == 0) {
            centroids = new double[0][];
            labels = new int[0];
            return;
        }
        int kk = Math.min(k, n);                         // "where feasible"
        int dim = points[0].length;
        Random rnd = new Random(seed);
        centroids = initialise(points, kk, rnd);
        labels = new int[n];
        Arrays.fill(labels, -1);

        for (iterations = 0; iterations < maxIterations; iterations++) {
            boolean changed = false;
            for (int i = 0; i < n; i++) {                // assignment step
                int best = nearest(points[i]);
                if (labels[i] != best) {
                    labels[i] = best;
                    changed = true;
                }
            }
            if (!changed) {
                break;                                   // converged
            }
            double[][] sum = new double[kk][dim];        // update step
            int[] count = new int[kk];
            for (int i = 0; i < n; i++) {
                count[labels[i]]++;
                for (int d = 0; d < dim; d++) {
                    sum[labels[i]][d] += points[i][d];
                }
            }
            for (int j = 0; j < kk; j++) {
                if (count[j] == 0) {                     // empty cluster: restart at the farthest point
                    centroids[j] = points[farthest(points)].clone();
                    continue;
                }
                for (int d = 0; d < dim; d++) {
                    centroids[j][d] = sum[j][d] / count[j];
                }
            }
        }
    }

    /** k-means++ style seeding: far-apart starting centroids. */
    private double[][] initialise(double[][] pts, int kk, Random rnd) {
        double[][] c = new double[kk][];
        c[0] = pts[rnd.nextInt(pts.length)].clone();
        for (int j = 1; j < kk; j++) {
            double[] d2 = new double[pts.length];
            double total = 0;
            for (int i = 0; i < pts.length; i++) {
                double best = Double.MAX_VALUE;
                for (int m = 0; m < j; m++) {
                    best = Math.min(best, dist2(pts[i], c[m]));
                }
                d2[i] = best;
                total += best;
            }
            int pick = rnd.nextInt(pts.length);
            if (total > 0) {
                double r = rnd.nextDouble() * total, acc = 0;
                for (int i = 0; i < pts.length; i++) {
                    acc += d2[i];
                    if (acc >= r) { pick = i; break; }
                }
            }
            c[j] = pts[pick].clone();
        }
        return c;
    }

    private int nearest(double[] p) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int j = 0; j < centroids.length; j++) {
            double d = dist2(p, centroids[j]);
            if (d < bd) { bd = d; best = j; }
        }
        return best;
    }

    private int farthest(double[][] pts) {
        int idx = 0;
        double bd = -1;
        for (int i = 0; i < pts.length; i++) {
            double d = dist2(pts[i], centroids[labels[i]]);
            if (d > bd) { bd = d; idx = i; }
        }
        return idx;
    }

    private static double dist2(double[] a, double[] b) {
        double s = 0;
        for (int d = 0; d < a.length; d++) {
            double diff = a[d] - b[d];
            s += diff * diff;
        }
        return s;
    }

    public double[][] getCentroids() { return centroids; }
    public int[] getLabels() { return labels; }
    public int getIterations() { return iterations; }
}
