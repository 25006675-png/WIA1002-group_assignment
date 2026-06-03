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
 * Reads routes.json and produces the graph, route schedule entries, and
 * simulated buses used by the UI.
 */
public class RouteLoader {
    public static final String DEMO_TIME = "09:08 AM";

    public static class LoadResult {
        public final BusGraph graph;
        public final List<SimulatedBus> buses;
        public final List<RouteEntry> routeEntries;

        public LoadResult(BusGraph graph, List<SimulatedBus> buses, List<RouteEntry> routeEntries) {
            this.graph = graph;
            this.buses = buses;
            this.routeEntries = routeEntries;
        }
    }

    public static class RouteEntry {
        public final String routeName;
        public final String busId;
        public final double speedKmPerHour;
        public final List<StopEntry> stops;
        public final List<TripEntry> trips;

        public RouteEntry(String routeName, String busId, double speedKmPerHour,
                          List<StopEntry> stops, List<TripEntry> trips) {
            this.routeName = routeName;
            this.busId = busId;
            this.speedKmPerHour = speedKmPerHour;
            this.stops = stops;
            this.trips = trips;
        }
    }

    public static class StopEntry {
        public final String name;
        public final double distanceFromStartKm;
        public final String scheduledTime;

        public StopEntry(String name, double distanceFromStartKm, String scheduledTime) {
            this.name = name;
            this.distanceFromStartKm = distanceFromStartKm;
            this.scheduledTime = scheduledTime;
        }
    }

    public static class TripEntry {
        public final String tripId;
        public final String startTime;
        public final List<String> stopTimes;

        public TripEntry(String tripId, String startTime, List<String> stopTimes) {
            this.tripId = tripId;
            this.startTime = startTime;
            this.stopTimes = stopTimes;
        }

        public String getEndTime() {
            return stopTimes.isEmpty() ? startTime : stopTimes.get(stopTimes.size() - 1);
        }
    }

    public static LoadResult load() {
        return load("routes.json");
    }

    public static LoadResult load(String resourcePath) {
        try {
            InputStream is = RouteLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new RuntimeException("routes.json not found on classpath.");
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

    private static String readFully(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString();
    }

    private static List<RouteEntry> parseRoutes(String json) {
        List<RouteEntry> routes = new ArrayList<>();
        String routesBlock = extractBlock(json, "\"routes\"");
        if (routesBlock.isEmpty()) return routes;

        String[] parts = routesBlock.split("\"routeName\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            String routeName = extractString(block, 0);
            String busId = extractStringAfterKey(block, "busId");
            double speed = extractDouble(block, "speedKmPerHour");
            List<StopEntry> stops = parseStops(block);
            List<TripEntry> trips = parseTrips(block, busId, stops);
            routes.add(new RouteEntry(routeName, busId, speed, stops, trips));
        }
        return routes;
    }

    private static List<StopEntry> parseStops(String routeBlock) {
        List<StopEntry> stops = new ArrayList<>();
        String stopsBlock = extractBlock(routeBlock, "\"stops\"");
        if (stopsBlock.isEmpty()) return stops;

        String[] parts = stopsBlock.split("\"name\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            String name = extractString(block, 0);
            double dist = extractDouble(block, "distanceFromStartKm");
            String schedTime = extractStringAfterKey(block, "scheduledTime");
            stops.add(new StopEntry(name, dist, schedTime));
        }
        return stops;
    }

    private static List<TripEntry> parseTrips(String routeBlock, String busId, List<StopEntry> stops) {
        List<TripEntry> trips = new ArrayList<>();
        String tripsBlock = extractBlock(routeBlock, "\"trips\"");

        if (!tripsBlock.isEmpty()) {
            String[] parts = tripsBlock.split("\"tripId\"");
            for (int i = 1; i < parts.length; i++) {
                String block = parts[i];
                String tripId = extractString(block, 0);
                String startTime = extractStringAfterKey(block, "startTime");
                List<String> stopTimes = extractStringArray(block, "stopTimes");
                trips.add(new TripEntry(tripId, startTime, stopTimes));
            }
        }

        if (trips.isEmpty()) {
            List<String> stopTimes = new ArrayList<>();
            for (StopEntry stop : stops) {
                if (!stop.scheduledTime.isEmpty()) stopTimes.add(stop.scheduledTime);
            }
            if (!stopTimes.isEmpty()) {
                String first = stopTimes.get(0);
                String suffix = first.replace(":", "").replace(" ", "");
                trips.add(new TripEntry(busId + "-" + suffix, first, stopTimes));
            }
        }
        return trips;
    }

    private static List<ConnectionEntry> parseConnections(String json) {
        List<ConnectionEntry> connections = new ArrayList<>();
        String connBlock = extractBlock(json, "\"connections\"");
        if (connBlock.isEmpty()) return connections;

        String[] parts = connBlock.split("\"from\"");
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            String from = extractString(block, 0);
            String to = extractStringAfterKey(block, "to");
            double distanceKm = extractDouble(block, "distanceKm");
            List<String> rids = extractStringArray(block, "routes");
            connections.add(new ConnectionEntry(from, to, distanceKm, rids));
        }
        return connections;
    }

    private static BusGraph buildGraph(List<RouteEntry> routes, List<ConnectionEntry> connections) {
        BusGraph graph = new BusGraph();
        for (RouteEntry route : routes) {
            for (StopEntry stop : route.stops) graph.addStop(stop.name);
        }
        for (ConnectionEntry conn : connections) {
            graph.addEdge(conn.from, conn.to, conn.distanceKm, conn.routeIds);
        }
        return graph;
    }

    private static List<SimulatedBus> buildBuses(List<RouteEntry> routes) {
        List<SimulatedBus> buses = new ArrayList<>();
        for (RouteEntry entry : routes) {
            buses.add(createSimulatedBus(entry, entry.busId));
        }
        return buses;
    }

    public static SimulatedBus createSimulatedBus(RouteEntry entry, String busId) {
        List<BusStop> stops = new ArrayList<>();
        for (StopEntry s : entry.stops) {
            stops.add(new BusStop(s.name, s.distanceFromStartKm));
        }
        BusRoute route = new BusRoute(entry.routeName, stops);
        return new SimulatedBus(busId, route, entry.speedKmPerHour);
    }

    public static int demoTimeMinutes() {
        return timeToMinutes(DEMO_TIME);
    }

    public static int timeToMinutes(String time) {
        String trimmed = time.trim().toUpperCase();
        String[] pieces = trimmed.split(" ");
        String[] hm = pieces[0].split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = Integer.parseInt(hm[1]);
        boolean pm = pieces.length > 1 && pieces[1].equals("PM");
        if (hour == 12) hour = 0;
        return (pm ? hour + 12 : hour) * 60 + minute;
    }

    public static String tripStatus(TripEntry trip) {
        int now = demoTimeMinutes();
        int start = timeToMinutes(trip.startTime);
        int end = timeToMinutes(trip.getEndTime());
        if (now < start) return "Upcoming";
        if (now <= end) return "Active";
        return "Completed";
    }

    public static int minutesUntil(String time) {
        return Math.max(0, timeToMinutes(time) - demoTimeMinutes());
    }

    private static String extractBlock(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = json.indexOf('[', idx);
        if (start < 0) return "";
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
        }
        return "";
    }

    private static String extractString(String s, int offset) {
        int q1 = s.indexOf('"', offset);
        if (q1 < 0) return "";
        int q2 = s.indexOf('"', q1 + 1);
        if (q2 < 0) return "";
        return s.substring(q1 + 1, q2);
    }

    private static String extractStringAfterKey(String s, String key) {
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        int colon = s.indexOf(':', idx);
        if (colon < 0) return "";
        return extractString(s, colon + 1);
    }

    private static double extractDouble(String s, String key) {
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int colon = s.indexOf(':', idx);
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < s.length() && s.charAt(start) == ' ') start++;
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.')) end++;
        try {
            return Double.parseDouble(s.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private static List<String> extractStringArray(String s, String key) {
        List<String> result = new ArrayList<>();
        int idx = s.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int arrStart = s.indexOf('[', idx);
        int arrEnd = s.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;
        String arrContent = s.substring(arrStart + 1, arrEnd);
        for (String part : arrContent.split(",")) {
            String val = part.trim().replace("\"", "");
            if (!val.isEmpty()) result.add(val);
        }
        return result;
    }

    private static class ConnectionEntry {
        final String from;
        final String to;
        final double distanceKm;
        final List<String> routeIds;

        ConnectionEntry(String from, String to, double distanceKm, List<String> routeIds) {
            this.from = from;
            this.to = to;
            this.distanceKm = distanceKm;
            this.routeIds = routeIds;
        }
    }
}
