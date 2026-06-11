package schedulensearch;

import routedata.RouteLoader;

/**
 * Tests for ScheduleWindow — hash-based timetable search.
 *
 * TC-U11: searchRoute('Campus Shuttle Blue') — lookup by route name
 * TC-U12: searchRoute('T818') — lookup by bus ID key (dual-key design)
 * TC-U13: searchStop('Main Gate') — lookup by stop name
 * TC-U14: searchRoute('Ghost Route') — missing key handled gracefully
 * TC-I6:  RouteLoader -> ScheduleWindow -> searchStop('Main Gate') returns all 4 routes
 */
public class ScheduleWindowTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) { System.out.println("  [PASS] " + name); passed++; }
        else           { System.out.println("  [FAIL] " + name); failed++; }
    }

    public static void main(String[] args) {
        System.out.println("=== ScheduleWindow (Hash Table) Tests ===\n");

        ScheduleWindow sw = new ScheduleWindow();

        testSearchRoute(sw);    // TC-U11, TC-U12
        testSearchStop(sw);     // TC-U13, TC-I6
        testMissingKeys(sw);    // TC-U14

        System.out.println("\nResults: " + passed + " passed, " + failed + " failed.");
    }

    // TC-U11: searchRoute() lookup by route name
    // TC-U12: searchRoute() lookup by bus ID key — dual-key design
    private static void testSearchRoute(ScheduleWindow sw) {
        System.out.println("TEST 1 [TC-U11, TC-U12]: searchRoute() — route name and bus ID lookup");

        // TC-U11: search by full route name
        String byName = sw.searchRoute("Campus Shuttle Blue");
        check("[TC-U11] Route name 'Campus Shuttle Blue' found in routeMap",
            byName.contains("TIMETABLE FOR"));
        check("[TC-U11] Result contains stop entries for the route",
            byName.contains("Main Gate"));

        // TC-U12: same data accessible via bus ID key
        String byId = sw.searchRoute("T818");
        check("[TC-U12] Bus ID 'T818' found in routeMap (dual-key design confirmed)",
            byId.contains("TIMETABLE FOR"));
        check("[TC-U12] Bus ID result contains same stop entries",
            byId.contains("Main Gate"));
    }

    // TC-U13: searchStop() lookup by stop name
    // TC-I6:  Integration — RouteLoader -> ScheduleWindow stopMap populated from routes.json
    private static void testSearchStop(ScheduleWindow sw) {
        System.out.println("\nTEST 2 [TC-U13, TC-I6]: searchStop() — stop name lookup");

        // TC-U13: basic stop lookup
        String result = sw.searchStop("Main Gate");
        check("[TC-U13] Stop 'Main Gate' found in stopMap",
            result.contains("UPCOMING BUSES AT"));

        // TC-I6: Integration — Main Gate is served by 4 routes (T818, X5, C3, HUB)
        // stopMap should contain entries from all 4 routes loaded from routes.json
        check("[TC-I6] Result contains Campus Shuttle Blue entries (T818 route)",
            result.contains("Campus Shuttle Blue"));
        check("[TC-I6] Result contains Campus Shuttle Red entries (X5 route)",
            result.contains("Campus Shuttle Red"));
        check("[TC-I6] Result contains Campus Shuttle Green entries (C3 route)",
            result.contains("Campus Shuttle Green"));
        check("[TC-I6] Result contains Hub Connector entries (HUB route)",
            result.contains("Hub Connector"));
    }

    // TC-U14: missing key returns graceful not-found message
    private static void testMissingKeys(ScheduleWindow sw) {
        System.out.println("\nTEST 3 [TC-U14]: Missing key — graceful not-found message");

        // TC-U14: unknown route key
        String badRoute = sw.searchRoute("Ghost Route");
        check("[TC-U14] Unknown route returns not-found message (no crash)",
            badRoute.contains("No route") || badRoute.contains("not found"));

        // Extra: unknown stop key (same pattern)
        String badStop = sw.searchStop("Ghost Stop");
        check("[TC-U14] Unknown stop returns not-found message (no crash)",
            badStop.contains("No stop") || badStop.contains("not found"));
    }
}