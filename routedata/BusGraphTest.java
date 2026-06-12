package routedata;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests for BusGraph — directed weighted graph of the campus bus network.
 *
 * TC-U6:  addEdge() with Main Gate connected to T818 and X5
 * TC-U7:  Cafeteria connected to C3 only — not a shared stop
 * TC-I1:  RouteLoader.load() builds graph with correct stop/edge count
 */
public class BusGraphTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) { System.out.println("  [PASS] " + name); passed++; }
        else           { System.out.println("  [FAIL] " + name); failed++; }
    }

    public static void main(String[] args) {
        System.out.println("=== BusGraph Tests ===\n");
        testGraphStructure();   // TC-U6
        testSharedStops();      // TC-U7
        testRealRouteData();    // TC-I1
        System.out.println("\nResults: " + passed + " passed, " + failed + " failed.");
    }

    // TC-U6: addEdge() registers stops and edges; getRoutesAtStop() returns correct routes
    private static void testGraphStructure() {
        System.out.println("TEST 1 [TC-U6]: addEdge() and adjacency list");
        BusGraph graph = new BusGraph();

        graph.addEdge("Main Gate", "Library", 1.5, Arrays.asList("T818"));
        graph.addEdge("Main Gate", "Student Centre", 2.0, Arrays.asList("X5"));

        // 2 addEdge calls with same 'from' -> 3 unique stops: Main Gate, Library, Student Centre
        check("[TC-U6] 3 stops registered (Main Gate, Library, Student Centre)", graph.getStopCount() == 3);
        check("[TC-U6] 2 edges registered", graph.getEdgeCount() == 2);
        check("[TC-U6] Main Gate has 2 outgoing edges", graph.getNeighbours("Main Gate").size() == 2);
        check("[TC-U6] Main Gate served by 2 routes", graph.getRoutesAtStop("Main Gate").size() == 2);
        check("[TC-U6] T818 is one of the routes at Main Gate", graph.getRoutesAtStop("Main Gate").contains("T818"));
        check("[TC-U6] X5 is one of the routes at Main Gate", graph.getRoutesAtStop("Main Gate").contains("X5"));
        check("[TC-U6] Main Gate flagged as shared stop", graph.getSharedStops().contains("Main Gate"));
    }

    // TC-U7: Single-route stop is NOT flagged as shared
    private static void testSharedStops() {
        System.out.println("\nTEST 2 [TC-U7]: Shared stop detection");
        BusGraph graph = new BusGraph();

        graph.addEdge("Main Gate", "Library", 1.5, Arrays.asList("T818"));
        graph.addEdge("Main Gate", "Student Centre", 2.0, Arrays.asList("X5"));
        graph.addEdge("Cafeteria", "Medical Centre", 1.3, Arrays.asList("C3"));

        check("[TC-U7] Cafeteria is NOT shared (only served by C3)", !graph.getSharedStops().contains("Cafeteria"));
        check("[TC-U7] Medical Centre is NOT shared (only served by C3)", !graph.getSharedStops().contains("Medical Centre"));
    }

    // TC-I1: Integration — RouteLoader builds correct graph from routes.json
    private static void testRealRouteData() {
        System.out.println("\nTEST 3 [TC-I1]: Integration — RouteLoader -> BusGraph from routes.json");
        RouteLoader.LoadResult result = RouteLoader.load();
        BusGraph graph = result.graph;

        check("[TC-I1] 13 unique stops loaded", graph.getStopCount() == 13);
        check("[TC-I1] 18 directed edges loaded", graph.getEdgeCount() == 18);
        check("[TC-I1] Main Gate served by 4 routes", graph.getRoutesAtStop("Main Gate").size() == 4);
        check("[TC-I1] Main Gate identified as shared stop", graph.getSharedStops().contains("Main Gate"));
        check("[TC-I1] Library identified as shared stop", graph.getSharedStops().contains("Library"));
    }
}