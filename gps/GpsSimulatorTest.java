package gps;

import java.util.Arrays;
import java.util.List;
import etacalculator.MultiBusSystem;
import etacalculator.BusArrival;
import routedata.RouteLoader;

/**
 * Tests for GpsSimulator and SimulatedBus.
 *
 * TC-U8:  tick() dequeues passed stops; currentStop and nextStop update correctly
 * TC-U9:  advance() caps at route end; isTripCompleted() triggers
 * TC-U10: advance(-1) throws IllegalArgumentException
 * TC-I2:  RouteLoader -> SimulatedBus -> GpsSimulator initial state (T818)
 * TC-I3:  RouteLoader -> GpsSimulator -> tick(120) updates stop correctly (T818)
 * TC-I4:  RouteLoader -> MultiBusSystem -> MinHeap sorted arrival order
 * TC-I5:  MultiBusSystem -> tickAll() -> MinHeap re-sorts after tick
 */
public class GpsSimulatorTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) { System.out.println("  [PASS] " + name); passed++; }
        else           { System.out.println("  [FAIL] " + name); failed++; }
    }

    // 3-stop route: Main Gate (0.0) -> Library (1.6) -> Student Centre (3.0), speed 60 km/h
    private static SimulatedBus buildTestBus() {
        List<BusStop> stops = Arrays.asList(
            new BusStop("Main Gate", 0.0),
            new BusStop("Library", 1.6),
            new BusStop("Student Centre", 3.0)
        );
        return new SimulatedBus("TEST-1", new BusRoute("Test Route", stops), 60.0);
    }

    public static void main(String[] args) {
        System.out.println("=== GpsSimulator & SimulatedBus Tests ===\n");
        testSimulatedBus();             // TC-U9, TC-U10
        testGpsSimulatorQueue();        // TC-U8
        testRealT818();                 // TC-I2, TC-I3
        testMultiBusSystemIntegration();// TC-I4, TC-I5
        System.out.println("\nResults: " + passed + " passed, " + failed + " failed.");
    }

    // TC-U9: advance() caps at route end and isTripCompleted() triggers
    // TC-U10: advance(-1) throws IllegalArgumentException
    private static void testSimulatedBus() {
        System.out.println("TEST 1 [TC-U9, TC-U10]: SimulatedBus — advance() and trip completion");
        SimulatedBus bus = buildTestBus();

        // TC-U9: advance far past route end (3.0 km); 60 km/h * 3600s = 60 km -> capped at 3.0
        bus.advance(3600);
        check("[TC-U9] Traveled distance capped at route end (3.0 km)", bus.getTraveledDistanceKm() == 3.0);
        check("[TC-U9] isTripCompleted() = true after reaching route end", bus.isTripCompleted());

        // TC-U10: negative elapsed time rejected
        try {
            buildTestBus().advance(-1);
            System.out.println("  [FAIL] [TC-U10] advance(-1) should throw IllegalArgumentException");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("  [PASS] [TC-U10] advance(-1) throws IllegalArgumentException");
            passed++;
        }
    }

    // TC-U8: Queue dequeues stop when bus passes it; currentStop and nextStop update
    private static void testGpsSimulatorQueue() {
        System.out.println("\nTEST 2 [TC-U8]: GpsSimulator — queue dequeue and stop updates");
        GpsSimulator sim = new GpsSimulator(buildTestBus());

        check("[TC-U8] Initial currentStop is Main Gate", sim.getStatus().getCurrentStop().equals("Main Gate"));
        check("[TC-U8] Initial nextStop is Library",      sim.getStatus().getNextStop().equals("Library"));

        // tick(96): 60 km/h * 96s = 1.6 km -> reaches Library exactly -> Library dequeued
        sim.tick(96);
        check("[TC-U8] After tick(96), currentStop updates to Library",
            sim.getStatus().getCurrentStop().equals("Library"));
        check("[TC-U8] After tick(96), nextStop updates to Student Centre",
            sim.getStatus().getNextStop().equals("Student Centre"));
        check("[TC-U8] Upcoming stops list reduced to 1 after passing Library",
            sim.getUpcomingStops().size() == 1);

        // tick(180): adds 3.0 km -> total 4.6 km -> capped at 3.0 -> trip complete
        sim.tick(180);
        check("[TC-U8] Trip completed after passing all stops", sim.getStatus().isTripCompleted());
        check("[TC-U8] nextStop shows 'No further stop'",
            sim.getStatus().getNextStop().equals("No further stop"));
    }

    // TC-I2: RouteLoader -> SimulatedBus -> GpsSimulator initial state
    // TC-I3: GpsSimulator tick(120) updates stop correctly
    private static void testRealT818() {
        System.out.println("\nTEST 3 [TC-I2, TC-I3]: Integration — RouteLoader -> T818 GpsSimulator");
        GpsSimulator sim = SampleDataFactory.createCampusShuttleSimulator();
        BusStatus status = sim.getStatus();

        // TC-I2: initial state loaded from routes.json
        check("[TC-I2] Bus ID is T818",                         status.getBusId().equals("T818"));
        check("[TC-I2] Initial currentStop is Main Gate",       status.getCurrentStop().equals("Main Gate"));
        check("[TC-I2] Initial nextStop is Faculty of Science", status.getNextStop().equals("Faculty of Science"));
        // 0.8 km at 24 km/h = ceil(2.0) = 2 min
        check("[TC-I2] ETA to Faculty of Science is 2 min",    status.getEtaToNextStopMinutes() == 2);

        // TC-I3: tick(120): 24 km/h * 120s = 0.8 km -> reaches Faculty of Science -> dequeued
        sim.tick(120);
        check("[TC-I3] After tick(120), currentStop is Faculty of Science",
            sim.getStatus().getCurrentStop().equals("Faculty of Science"));
        check("[TC-I3] After tick(120), nextStop is Library",
            sim.getStatus().getNextStop().equals("Library"));
    }

    // TC-I4: MultiBusSystem loads all buses and returns sorted arrival order
    // TC-I5: After tickAll(), arrival order re-sorts correctly
    private static void testMultiBusSystemIntegration() {
        System.out.println("\nTEST 4 [TC-I4, TC-I5]: Integration — MultiBusSystem -> MinHeap");

        MultiBusSystem system = new MultiBusSystem();
        for (SimulatedBus bus : RouteLoader.load().buses) {
            system.addBus(new GpsSimulator(bus));
        }

        // TC-I4: all 5 buses returned in ascending ETA order
        List<BusArrival> order1 = system.getBusesInArrivalOrder();
        check("[TC-I4] All 5 buses returned", order1.size() == 5);
        check("[TC-I4] Sorted ascending by ETA (first <= last)",
            order1.get(0).getEtaToNextStopMin() <= order1.get(order1.size() - 1).getEtaToNextStopMin());

        // TC-I5: after tickAll(300), order updates to reflect new ETAs
        system.tickAll(300);
        List<BusArrival> order2 = system.getBusesInArrivalOrder();
        check("[TC-I5] After tickAll(300), still 5 buses in system", order2.size() == 5);
        check("[TC-I5] After tickAll(300), result still sorted ascending",
            order2.get(0).getEtaToNextStopMin() <= order2.get(order2.size() - 1).getEtaToNextStopMin());
    }
}