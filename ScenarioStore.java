package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.GridCell;
import model.SightingReport;
import simulation.Scenario;

/** Optional persistence: save/load a scenario as a small text file. UNIT 5: try / catch / finally / throw. */
public final class ScenarioStore {
    private ScenarioStore() { }

    public static void save(Scenario sc, File file) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write("seed=" + sc.getSeed()); out.newLine();
            out.write("k=" + sc.getClusterCount()); out.newLine();
            out.write("reserve=" + sc.getReservePercent()); out.newLine();
            StringBuilder b = new StringBuilder();
            for (double v : sc.getBatteryLevels()) b.append(b.length() == 0 ? "" : ",").append(v);
            out.write("batteries=" + b); out.newLine();
            for (SightingReport r : sc.getReports()) {
                out.write("report=" + r.getCell().getRow() + "," + r.getCell().getCol() + ","
                        + r.getConfidence() + "," + r.getMinutes());
                out.newLine();
            }
            for (GridCell g : sc.getMap().getRestrictedCells()) {
                out.write("restricted=" + g.getRow() + "," + g.getCol());
                out.newLine();
            }
        } finally {
            if (out != null) {
                out.close();                  // always runs, even if a write failed
            }
        }
    }

    public static void load(Scenario sc, File file) throws IOException {
        long seed = 1;
        int k = 4;
        double reserve = 20;
        double[] batt = {100, 100, 100};
        List<SightingReport> reports = new ArrayList<>();
        List<GridCell> restricted = new ArrayList<>();
        int id = 1;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String[] v = line.substring(eq + 1).trim().split(",");
                try {
                    switch (key) {
                        case "seed": seed = Long.parseLong(v[0]); break;
                        case "k": k = Integer.parseInt(v[0]); break;
                        case "reserve": reserve = Double.parseDouble(v[0]); break;
                        case "batteries":
                            batt = new double[v.length];
                            for (int i = 0; i < v.length; i++) batt[i] = Double.parseDouble(v[i]);
                            break;
                        case "report":
                            reports.add(new SightingReport(id++, new GridCell(Integer.parseInt(v[0]), Integer.parseInt(v[1])),
                                    Double.parseDouble(v[2]), Integer.parseInt(v[3])));
                            break;
                        case "restricted":
                            restricted.add(new GridCell(Integer.parseInt(v[0]), Integer.parseInt(v[1])));
                            break;
                        default: break;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {   // multi-catch
                    throw new IOException("Bad line in scenario file: " + line, e);        // rethrow as checked
                }
            }
        }
        try {
            sc.restore(seed, k, reserve, batt, reports, restricted);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid scenario values: " + e.getMessage(), e);
        }
    }
}
