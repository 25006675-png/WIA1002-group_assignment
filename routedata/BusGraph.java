package routedata;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
 
/**
 * Graph data structure for the campus bus network.
 *
 * Each bus stop is a NODE.
 * Each road segment between two stops is a directed EDGE with a distance weight.
 *
 * Internally uses an adjacency list: HashMap<stopName, List<Edge>>
 * This gives O(1) lookup for neighbours of any stop.
 *
 * Data structures used inside this class:
 *   - HashMap     : adjacency list (stop name -> list of edges)
 *   - HashSet     : shared stop detection (stops appearing in 2+ routes)
 *   - LinkedList  : used as Queue for BFS path finding
 *   - List        : neighbour lists and path results
 */
public class BusGraph {
 
    // ── Inner classes ─────────────────────────────────────────────────────────
 
    /** A directed weighted edge from one stop to another. */
    public static class Edge {
        private final String from;
        private final String to;
        private final double distanceKm;
        private final List<String> routeIds;
 
        public Edge(String from, String to, double distanceKm, List<String> routeIds) {
            this.from     = from;
            this.to       = to;
            this.distanceKm = distanceKm;
            this.routeIds = Collections.unmodifiableList(new ArrayList<>(routeIds));
        }
 
        public String getFrom()           { return from; }
        public String getTo()             { return to; }
        public double getDistanceKm()     { return distanceKm; }
        public List<String> getRouteIds() { return routeIds; }
 
        @Override
        public String toString() {
            return from + " -> " + to + " (" + distanceKm + " km) " + routeIds;
        }
    }
 
    /** One step in a found path: which stop, which edge got us here. */
    public static class PathStep {
        private final String stopName;
        private final Edge via; // null for the starting stop
 
        public PathStep(String stopName, Edge via) {
            this.stopName = stopName;
            this.via      = via;
        }
 
        public String getStopName() { return stopName; }
        public Edge   getVia()      { return via; }
 
        @Override
        public String toString() {
            if (via == null) return "[START] " + stopName;
            return stopName + " via " + via.getRouteIds() + " (" + via.getDistanceKm() + " km)";
        }
    }
 
    // ── Fields ────────────────────────────────────────────────────────────────
 
    /** Adjacency list: stop name -> outgoing edges. */
    private final Map<String, List<Edge>> adjacencyList;
 
    /** Tracks which routes serve each stop — used for shared stop detection. */
    private final Map<String, Set<String>> stopRouteMap;
 
    // ── Constructor ───────────────────────────────────────────────────────────
 
    public BusGraph() {
        this.adjacencyList = new HashMap<>();
        this.stopRouteMap  = new HashMap<>();
    }
 
    // ── Mutation ──────────────────────────────────────────────────────────────
 
    /**
     * Add a stop (node). Safe to call multiple times — duplicates ignored.
     */
    public void addStop(String stopName) {
        if (stopName == null || stopName.isBlank())
            throw new IllegalArgumentException("Stop name cannot be empty.");
        adjacencyList.putIfAbsent(stopName, new ArrayList<>());
        stopRouteMap.putIfAbsent(stopName, new HashSet<>());
    }
 
    /**
     * Add a directed edge between two stops.
     * Both stops are auto-registered if not already present.
     *
     * @param from       origin stop name
     * @param to         destination stop name
     * @param distanceKm road distance in km
     * @param routeIds   bus routes that use this edge
     */
    public void addEdge(String from, String to, double distanceKm, List<String> routeIds) {
        if (distanceKm < 0)
            throw new IllegalArgumentException("Distance cannot be negative.");
        addStop(from);
        addStop(to);
        adjacencyList.get(from).add(new Edge(from, to, distanceKm, routeIds));
        for (String rid : routeIds) {
            stopRouteMap.get(from).add(rid);
            stopRouteMap.get(to).add(rid);
        }
    }
 
    // ── Queries ───────────────────────────────────────────────────────────────
 
    /** All stop names in the graph. */
    public Set<String> getAllStops() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }
 
    /** Outgoing edges from a stop. Empty list if none. */
    public List<Edge> getNeighbours(String stopName) {
        List<Edge> edges = adjacencyList.get(stopName);
        return edges == null ? Collections.emptyList() : Collections.unmodifiableList(edges);
    }
 
    /** True if the stop exists in the graph. */
    public boolean hasStop(String stopName) {
        return adjacencyList.containsKey(stopName);
    }
 
    /** Number of stops (nodes). */
    public int getStopCount() { return adjacencyList.size(); }
 
    /** Total number of directed edges. */
    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }
 
    /**
     * Returns stops served by more than one route — the transfer points.
     * e.g. "Main Gate" is shared by T818, X5, C3, HUB.
     */
    public List<String> getSharedStops() {
        List<String> shared = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : stopRouteMap.entrySet()) {
            if (entry.getValue().size() > 1) shared.add(entry.getKey());
        }
        Collections.sort(shared);
        return shared;
    }
 
    /**
     * Returns the set of route IDs that serve a given stop.
     * e.g. "Library" -> ["T818", "I1", "HUB"]
     */
    public Set<String> getRoutesAtStop(String stopName) {
        Set<String> routes = stopRouteMap.get(stopName);
        return routes == null ? Collections.emptySet() : Collections.unmodifiableSet(routes);
    }
 
    /**
     * BFS shortest-hop path from start stop to end stop.
     * Uses LinkedList as a Queue (FIFO) to explore stops level by level.
     *
     * Returns an ordered list of PathSteps, or empty list if unreachable.
     */
    public List<PathStep> findPath(String start, String end) {
        if (!hasStop(start) || !hasStop(end)) return Collections.emptyList();
        if (start.equals(end)) {
            List<PathStep> trivial = new ArrayList<>();
            trivial.add(new PathStep(start, null));
            return trivial;
        }
 
        Queue<String> queue   = new LinkedList<>();
        Map<String, PathStep> visited = new HashMap<>();
 
        queue.add(start);
        visited.put(start, new PathStep(start, null));
 
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (Edge edge : getNeighbours(current)) {
                String next = edge.getTo();
                if (!visited.containsKey(next)) {
                    visited.put(next, new PathStep(next, edge));
                    queue.add(next);
                    if (next.equals(end)) return reconstructPath(visited, start, end);
                }
            }
        }
        return Collections.emptyList();
    }
 
    /** Reconstructs the path by walking back through the visited map. */
    private List<PathStep> reconstructPath(Map<String, PathStep> visited, String start, String end) {
        LinkedList<PathStep> path = new LinkedList<>();
        String current = end;
        while (!current.equals(start)) {
            PathStep step = visited.get(current);
            path.addFirst(step);
            current = step.getVia().getFrom();
        }
        path.addFirst(new PathStep(start, null));
        return new ArrayList<>(path);
    }
 
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusGraph [").append(getStopCount()).append(" stops, ")
          .append(getEdgeCount()).append(" edges]\n");
        List<String> stops = new ArrayList<>(adjacencyList.keySet());
        Collections.sort(stops);
        for (String stop : stops) {
            Set<String> routes = stopRouteMap.get(stop);
            sb.append("  ").append(stop);
            if (routes.size() > 1) sb.append(" [SHARED: ").append(routes).append("]");
            sb.append("\n");
            for (Edge e : adjacencyList.get(stop))
                sb.append("    -> ").append(e).append("\n");
        }
        return sb.toString();
    }
}