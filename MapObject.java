package model;

/**
 * Root of everything that sits on the map.
 * UNIT 3: abstract class, protected field, final method.
 * UNIT 4: the hierarchy  MapObject -> Aircraft -> Drone  is multi-level inheritance.
 */
public abstract class MapObject {
    private final int id;
    protected GridCell cell;

    protected MapObject(int id, GridCell cell) {
        this.id = id;
        this.cell = cell;
    }

    public final int getId() { return id; }          // final: subclasses cannot override
    public GridCell getCell() { return cell; }
    public void setCell(GridCell cell) { this.cell = cell; }

    public abstract String getLabel();               // abstract: every subclass must define it

    @Override
    public String toString() { return getLabel() + "@" + cell; }
}
