package gps;

import routedata.RouteLoader;
import java.util.List;

public final class SampleDataFactory {
    private SampleDataFactory() {}

    public static List<SimulatedBus> getAllBuses() {
        return RouteLoader.load().buses;
    }

    public static GpsSimulator createCampusShuttleSimulator() {
        return RouteLoader.load().buses.stream()
            .filter(b -> b.getBusId().equals("T818"))
            .findFirst()
            .map(GpsSimulator::new)
            .orElseThrow(() -> new RuntimeException("T818 not found in routes.json"));
    }
}