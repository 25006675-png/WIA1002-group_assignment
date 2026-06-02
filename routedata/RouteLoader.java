package routedata;
 
import gps.BusRoute;
import gps.BusStop;
import gps.SimulatedBus;
 
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Reads routes.json and produces:
 *   1. A BusGraph (the graph data structure for the whole campus network)
 *   2. A list of SimulatedBus objects (ready to plug into GpsSimulator)
 *
 * This makes routes.json the single source of truth — SampleDataFactory
 * and ScheduleWindow should be updated to delegate here instead of
 * hardcoding their own stop lists.
 *
 * NOTE: This parser is intentionally written without any external JSON
 * library (no Gson, no Jackson) so the project has zero extra dependencies.
 * It uses simple line-by-line string parsing sufficient for the fixed
 * structure of routes.json.
 */
public class RouteLoader {
 
    // ── Public result holder ──────────────────────────────────────────────────
 
    /** Holds everything produced by a single load() call. */
    public static class LoadResult {
        public final BusGraph graph;
        public final List<SimulatedBus> buses;
        public final List<RouteEntry> routeEntries;
 
        public LoadResult(BusGraph graph, List<SimulatedBus> buses, List<RouteEntry> routeEntries) {
            this.graph        = graph;
            this.buses        = buses;
            this.routeEntries = routeEntries;
        }
    }
 
    /** Raw route data parsed from JSON — useful for ScheduleWindow. */
    public static class RouteEntry {
        public final String routeName;
        public final String busId;
        public final double speedKmPerHour;
        public final List<StopEntry> stops;
 
        public RouteEntry(String routeName, String busId, double speedKmPerHour, List<StopEntry> stops) {
            this.routeName      = routeName;
            this.busId          = busId;
            this.speedKmPerHour = speedKmPerHour;
            this.stops          = stops;
        }
    }
 
    /** Raw stop data parsed from JSON. */
    public static class StopEntry {
        public final String name;
        public final double distanceFromStartKm;
        public final String scheduledTime;
 
        public StopEntry(String name, double distanceFromStartKm, String scheduledTime) {
            this.name                = name;
            this.distanceFromStartKm = distanceFromStartKm;
            this.scheduledTime       = scheduledTime;
        }
    }
 
    // ── Main load method ──────────────────────────────────────────────────────
 
    /**
     * Loads routes.json from the classpath and returns a LoadResult.
     *
     * Place routes.json in your project's resources root so it is on the
     * classpath at runtime (NetBeans: right-click project -> Properties ->
     * Sources -> add the data/ folder as a source root, or copy routes.json
     * into src/).
     *
     * @return LoadResult containing graph, buses, and raw route entries
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public static LoadResult load() {
        return load("routes.json");
    }
 
    /**
     * Loads a named JSON file from the classpath.
     * Exposed for testing with a different file path.
     */
    public static LoadResult load(String resourcePath) {
        try {
            InputStream is = RouteLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new RuntimeException(
                    "routes.json not found on classpath. " +
                    "Make sure data/routes.json is included as a resource.");
            }
 
            String json = readFully(is);
            List<RouteEntry> routeEntries = parseRoutes(json);
            List<ConnectionEntry> connections = parseConnections(json);
 
            BusGraph graph = buildGraph(routeEntries, connections);
            List<SimulatedBus> buses = buildBuses(routeEntries);
 
            return new LoadResult(graph, buses, routeEntries);
 
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load routes.json: " + e.getMessage(), e);
        }
    }
 
    // ── Parsers ───────────────────────────────────────────────────────────────
 
    private static String readFully(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString();
    }
 
    /** Parses all route blocks from the JSON string. */
    private static List<RouteEntry> parseRoutes(String json) {
        List<RouteEntry> routes = new ArrayList<>();
 
        // Extract the "routes" array block
        String routesBlock = extractBlock(json, "\"routes\"");
        if (routesBlock.isEmpty()) return routes;
 
        // Split into individual route objects by "routeName"
        String[] parts = routesBlock.split("\"routeName\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
 
            String routeName      = extractString(block, 0);
            String busId          = extractStringAfterKey(block, "busId");
            double speed          = extractDouble(block, "speedKmPerHour");
            List<StopEntry> stops = parseStops(block);
 
            routes.add(new RouteEntry(routeName, busId, speed, stops));
        }
        return routes;
    }
 
    /** Parses stop entries inside a route block. */
    private static List<StopEntry> parseStops(String routeBlock) {
        List<StopEntry> stops = new ArrayList<>();
        String[] parts = routeBlock.split("\"name\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            // Stop if we've gone past the stops array into connections
            if (block.contains("\"connections\"")) break;
 
            String name      = extractString(block, 0);
            double dist      = extractDouble(block, "distanceFromStartKm");
            String schedTime = extractStringAfterKey(block, "scheduledTime");
            stops.add(new StopEntry(name, dist, schedTime));
        }
        return stops;
    }
 
    /** Parses all connection blocks from the JSON string. */
    private static List<ConnectionEntry> parseConnections(String json) {
        List<ConnectionEntry> connections = new ArrayList<>();
        String connBlock = extractBlock(json, "\"connections\"");
        if (connBlock.isEmpty()) return connections;
 
        String[] parts = connBlock.split("\"from\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            String from       = extractString(block, 0);
            String to         = extractStringAfterKey(block, "to");
            double distanceKm = extractDouble(block, "distanceKm");
            List<String> rids = extractStringArray(block, "routes");
            connections.add(new ConnectionEntry(from, to, distanceKm, rids));
        }
        return connections;
    }
 
    // ── Builders ──────────────────────────────────────────────────────────────
 
    private static BusGraph buildGraph(List<RouteEntry> routes, List<ConnectionEntry> connections) {
        BusGraph graph = new BusGraph();
 
        // Add all stops as nodes first
        for (RouteEntry route : routes) {
            for (StopEntry stop : route.stops) {
                graph.addStop(stop.name);
            }
        }
 
        // Add edges from connections block
        for (ConnectionEntry conn : connections) {
            graph.addEdge(conn.from, conn.to, conn.distanceKm, conn.routeIds);
        }
 
        return graph;
    }
 
    private static List<SimulatedBus> buildBuses(List<RouteEntry> routes) {
        List<SimulatedBus> buses = new ArrayList<>();
        for (RouteEntry entry : routes) {
            List<BusStop> stops = new ArrayList<>();
            for (StopEntry s : entry.stops) {
                stops.add(new BusStop(s.name, s.distanceFromStartKm));
            }
            // HUB route ends at same stop as start — trim the duplicate last stop
            // so BusRoute validation (ascending distances) still passes
            if (entry.busId.equals("HUB") && stops.size() > 1) {
                String first = stops.get(0).getName();
                String last  = stops.get(stops.size() - 1).getName();
                if (first.equals(last)) stops.remove(stops.size() - 1);
            }
            BusRoute route = new BusRoute(entry.routeName, stops);
            buses.add(new SimulatedBus(entry.busId, route, entry.speedKmPerHour));
        }
        return buses;
    }
 
    // ── Minimal JSON helpers ──────────────────────────────────────────────────
 
    /** Extracts the array/object block that follows a given key. */
    private static String extractBlock(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = json.indexOf('[', idx);
        if (start < 0) return "";
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "";
    }
 
    /** Extracts the first quoted string value starting from offset. */
    private static String extractString(String s, int offset) {
        int q1 = s.indexOf('"', offset);
        if (q1 < 0) return "";
        int q2 = s.indexOf('"', q1 + 1);
        if (q2 < 0) return "";
        return s.substring(q1 + 1, q2);
    }
 
    /** Extracts the string value for a given key. */
    private static String extractStringAfterKey(String s, String key) {
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        int colon = s.indexOf(':', idx);
        if (colon < 0) return "";
        return extractString(s, colon + 1);
    }
 
    /** Extracts a double value for a given key. */
    private static double extractDouble(String s, String key) {
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int colon = s.indexOf(':', idx);
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < s.length() && (s.charAt(start) == ' ')) start++;
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.')) end++;
        try { return Double.parseDouble(s.substring(start, end)); } catch (Exception e) { return 0; }
    }
 
    /** Extracts a JSON string array value for a given key. */
    private static List<String> extractStringArray(String s, String key) {
        List<String> result = new ArrayList<>();
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int arrStart = s.indexOf('[', idx);
        int arrEnd   = s.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;
        String arrContent = s.substring(arrStart + 1, arrEnd);
        for (String part : arrContent.split(",")) {
            String val = part.trim().replace("\"", "");
            if (!val.isEmpty()) result.add(val);
        }
        return result;
    }
 
    // ── Private helper class ──────────────────────────────────────────────────
 
    private static class ConnectionEntry {
        final String from;
        final String to;
        final double distanceKm;
        final List<String> routeIds;
 
        ConnectionEntry(String from, String to, double distanceKm, List<String> routeIds) {
            this.from       = from;
            this.to         = to;
            this.distanceKm = distanceKm;
            this.routeIds   = routeIds;
        }
    }
}