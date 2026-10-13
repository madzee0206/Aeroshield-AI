# Aeroshield-AI
Java swing swarm drone search planner

**A Java-Based Intelligent Drone Swarm Simulation System for Police Response and Post-Incident Search**

AeroShield AI is a Java desktop application that simulates how a fleet of drones can help police coordinate a search
after a reported bank theft. It combines synthetic incident reports, K-Means clustering, greedy drone-zone assignment,
Dijkstra shortest-path routing and simplified flight physics, and shows the result on a fictional city map with a
dark glassmorphism interface.

> Clusters represent groups of reports, **not** proof of a suspect's location. All data is fictional and generated locally.
> No real maps, personal data, live GPS or surveillance are used.

Built with Java Swing and Graphics2D only. **No external libraries.**

---

## 1. Requirements
- Java JDK 11 or newer (developed and tested with Java 21)
- Any operating system with a desktop (Windows, macOS, Linux)

## 2. Build and run

From inside the `AeroShieldAI` folder:

```bash
mkdir out
javac -d out $(find . -name "*.java")     # Windows PowerShell: javac -d out (Get-ChildItem -Recurse -Filter *.java).FullName
java -cp out MainApp                       # start the application
java -cp out tests.AeroShieldTests         # run the automated tests (21 checks, no JUnit needed)
```

On Linux/macOS you can also run `./run.sh`.
If you update any file, delete the `out` folder first so no old classes remain.

`-cp out` is the **class path**: it tells the JVM where the compiled packages are.

## 3. Using the application

| Screen | What it does |
|---|---|
| **Simulation** | City map, metric cards, fleet roster with battery bars, live event feed, bottom controls |
| **Fleet** | Table of drones: status, battery, assigned zones, distance flown |
| **Reports** | Table of incident reports: cell, confidence, time, cluster |
| **Analytics** | Metric cards, grouped bar chart (greedy vs baseline) and an interactive 3-D chart |
| **Settings** | Clusters (K), battery reserve, drone batteries, strategy, seed, save/load |

**Map controls**
- Left-click a road/open cell: add or remove a report (buildings are rejected).
- Right-click a road/open cell: toggle a no-fly (restricted) cell.
- Editing is only possible when the simulation is in the READY state; press RESET first otherwise.

**Bottom controls:** START, PAUSE, RESUME, RESET, speed slider (1x-16x), NEW SCENARIO, RE-PLAN.

**3-D chart (Analytics):** drag to rotate, mouse wheel to zoom, double-click to reset.
Buttons: SHOW BASELINE / SHOW GREEDY, AUTO-ROTATE, RESET VIEW.
Axes: drones x metric (time, energy, distance); each metric row is scaled to its own maximum.

## 4. How it works

| Component | Method | Purpose |
|---|---|---|
| Report grouping | K-Means (k-means++ seeding, seeded random) | Group nearby report coordinates into survey zones |
| Drone-zone assignment | Greedy cost-based | Heaviest zone first; each goes to the feasible drone with the lowest cost |
| Baseline | Round-robin | Deals zones to drones in turn, ignoring workload and distance |
| Pathfinding | Dijkstra (4-neighbour grid, equal costs) | Shortest valid route; reports unreachable destinations |
| Flight time | t = d / v | Estimate travel duration |
| Energy | E = P x t | Estimate energy use (cruise and hover power) |
| Safety | Battery reserve | Required energy + return reserve must fit in the drone's charge |

**Greedy cost** = projected finish time (seconds) + 3 x battery use (%). A zone with no feasible drone is flagged, not assigned.
At run time a safety check aborts a mission and routes the drone home with Dijkstra if charge drops too low.

Physical assumptions: 50 m per grid cell, 10 m/s speed, 200 W cruise, 150 W hover, 50 Wh battery, 20% default reserve.

## 5. Project structure

```
AeroShieldAI/
├── MainApp.java
├── theme/        Palette, Fonts, Theme, Icons
├── components/   GlassPanel, MetricCard, StatusBadge
├── ui/           MainFrame, SimulationScreen, AnalyticsScreen, Chart3DPanel,
│                 FleetPanel, TableScreen, SettingsScreen
├── map/          CityMap, CityMapRenderer
├── model/        MapObject, Aircraft, Drone, SightingReport, SurveyZone,
│                 GridCell, Segment, CellType, DroneStatus, Describable, Resettable
├── ml/           KMeans
├── planning/     ZoneAssignment, GreedyAssignment, RoundRobinAssignment,
│                 ZoneBuilder, Plan, DroneRoute
├── pathfinding/  Dijkstra, NoPathException
├── physics/      FlightModel, InfeasibleMissionException
├── simulation/   Scenario, SimulationEngine, EventLog
├── analytics/    MetricsCalculator
├── io/           ScenarioStore
└── tests/        AeroShieldTests
```

## 6. Java concepts used (course syllabus)

| Unit | Topic | Where |
|---|---|---|
| 1 | Primitives, objects | `FlightModel`, `GridCell`, all `new ...` |
| 2 | if-else, for, while, switch, break, continue | `KMeans.fit`, `Dijkstra`, `CityMapRenderer` (switch on enum), `CityMap.nearestPassable`, `ScenarioStore.load` |
| 3 | Access modifiers | `MapObject` (private id, protected cell), `DroneRoute.finish` (package-private) |
| 3 | Constructors, `this(...)` | `SightingReport`, `Drone`, `GlassPanel`, `KMeans` |
| 3 | static, final | `Palette`, `Fonts` (constants), `FlightModel` (final class), `MapObject.getId()` (final method) |
| 3 | abstract | `MapObject`, `Aircraft`, `ZoneAssignment` |
| 4 | Object class, equals/hashCode/toString | `GridCell`, `MapObject` |
| 4 | super, multi-level inheritance | `MapObject -> Aircraft -> Drone`; `JPanel -> GlassPanel -> MetricCard` |
| 4 | Typecasting, overriding | `CityMapRenderer` (`instanceof` + downcast), `getLabel()` overrides |
| 4 | Final and abstract classes | `FlightModel`; `ZoneAssignment.assign()` (final template method) + abstract `choose()` |
| 5 | 1-D and 2-D arrays | `KMeans`, `CityMap`, `Dijkstra`, `Chart3DPanel` |
| 5 | Inheritance in arrays | `Scenario.allObjects()` returns a `MapObject[]` of zones, reports and drones |
| 5 | Exceptions | Checked: `NoPathException`, `InfeasibleMissionException`. Unchecked: `IllegalArgumentException`. `try/catch/throw/finally` in `ScenarioStore` |
| 5 | Packages, imports, class path | 12 packages; single, wildcard and static imports; `-cp out` |
| 5 | Interfaces (multiple inheritance) | `Drone implements Describable, Resettable` |
| 6 | Swing | `ui/`, `components/` |
| 6 | Collections | `ArrayList`, `HashMap`, `HashSet`, `PriorityQueue`, `LinkedHashMap`, `CopyOnWriteArrayList`, `Comparator` |
| 6 | Multithreading | `Scenario.replanAsync` (worker thread, `synchronized`, `volatile`), `EventLog` (synchronized), Swing `Timer`, `invokeLater` |

## 7. Testing

`java -cp out tests.AeroShieldTests` runs 21 checks covering: empty scenario, add/remove report, K-Means grouping,
blocked and alternate routes, Dijkstra minimum cost, zone-assignment feasibility, low battery, pause/resume, reset,
analytics vs simulated data, and reproducibility with the same seed.

## 8. Reproducing the report figures

Start the app with the defaults (scenario **BANK THEFT CASE #002**, seed 2, K = 5, batteries 100,95,90,70, reserve 20).
Take screenshots on the Simulation screen **before** pressing START.

**Figure A.9 - Zone B has no drone with enough battery**
1. Settings: set *Drone batteries %* to `55,55,55,55` (K stays 5).
2. Click APPLY, then open Simulation.
3. Event feed: `WARNING Zone B: rejected - no drone has enough battery (incl. return reserve)`.

**Figure A.10 - Eight zones and Zone H unreachable**
1. Settings: set *Number of clusters (K)* to `8` (batteries stay `100,95,90,70`).
2. Click APPLY, then open Simulation.
3. Eight zones (A-H) appear; event feed: `WARNING Zone H: unreachable from base (blocked by buildings / restricted cells)`.
   Zone H sits on a park cell that is completely enclosed by buildings.

**Greedy vs baseline (Analytics):** with the defaults, greedy completes in about 5.9 min and the baseline in about 10.3 min.

## 9. Optional: saving scenarios
Settings -> SAVE writes a small text file (seed, K, reserve, batteries, reports, restricted cells). LOAD restores it.

## 10. Limitations and possible extensions
- Equal movement cost on all cells (terrain costs can be added in `CellType` / `Dijkstra`).
- No collision avoidance between drones.
- Not intended for real-world operations; all data is synthetic.
