package model;

/** Abstract middle layer: anything with a battery. UNIT 4: abstract class, protected members. */
public abstract class Aircraft extends MapObject {
    private final double capacityJoules;
    private final double initialEnergyJoules;
    private double energyJoules;

    protected Aircraft(int id, GridCell cell, double capacityJoules, double startPercent) {
        super(id, cell);
        if (startPercent < 0 || startPercent > 100) {
            throw new IllegalArgumentException("battery percent must be 0..100");
        }
        this.capacityJoules = capacityJoules;
        this.initialEnergyJoules = capacityJoules * startPercent / 100.0;
        this.energyJoules = initialEnergyJoules;
    }

    public abstract String getModelName();

    public double getCapacityJoules() { return capacityJoules; }
    public double getInitialEnergyJoules() { return initialEnergyJoules; }
    public double getEnergyJoules() { return energyJoules; }
    public double getBatteryPercent() { return 100.0 * energyJoules / capacityJoules; }

    public void drain(double joules) { energyJoules = Math.max(0, energyJoules - joules); }
    protected void restoreEnergy() { energyJoules = initialEnergyJoules; }
}
