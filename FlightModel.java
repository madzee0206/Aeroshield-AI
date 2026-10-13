package physics;

/**
 * Simplified flight physics (FR-06):  t = d / v   and   E = P * t.
 * UNIT 3: final class, immutable fields, static factory.
 */
public final class FlightModel {
    private final double speedMps;
    private final double cruisePowerW;
    private final double hoverPowerW;
    private final double cellMeters;

    public FlightModel(double speedMps, double cruisePowerW, double hoverPowerW, double cellMeters) {
        if (speedMps <= 0 || cruisePowerW <= 0 || hoverPowerW <= 0 || cellMeters <= 0) {
            throw new IllegalArgumentException("all flight parameters must be positive");
        }
        this.speedMps = speedMps;
        this.cruisePowerW = cruisePowerW;
        this.hoverPowerW = hoverPowerW;
        this.cellMeters = cellMeters;
    }

    public static FlightModel standard() {
        return new FlightModel(10.0, 200.0, 150.0, 50.0);
    }

    public double flightTime(double distanceMeters) { return distanceMeters / speedMps; }        // t = d/v
    public double energy(double powerWatts, double seconds) { return powerWatts * seconds; }     // E = P*t
    public double cellsToMeters(double cells) { return cells * cellMeters; }
    public double secondsPerCell() { return cellMeters / speedMps; }

    public double flightEnergyForCells(double cells) {
        return energy(cruisePowerW, flightTime(cellsToMeters(cells)));
    }

    public double hoverEnergy(double seconds) { return energy(hoverPowerW, seconds); }

    /** Energy for a whole mission: total flown cells + total hover time. */
    public double requiredEnergy(double totalCells, double surveySeconds) {
        return flightEnergyForCells(totalCells) + hoverEnergy(surveySeconds);
    }

    /** Throws if required + reserve exceeds what the battery holds. */
    public void checkMission(double availableJoules, double requiredJoules, double reserveJoules)
            throws InfeasibleMissionException {
        double need = requiredJoules + reserveJoules;
        if (need > availableJoules) {
            throw new InfeasibleMissionException(
                    String.format("Needs %.1f Wh (incl. reserve) but only %.1f Wh available",
                            need / 3600.0, availableJoules / 3600.0),
                    need - availableJoules);
        }
    }

    public double getSpeedMps() { return speedMps; }
    public double getCruisePowerW() { return cruisePowerW; }
    public double getHoverPowerW() { return hoverPowerW; }
    public double getCellMeters() { return cellMeters; }
}
