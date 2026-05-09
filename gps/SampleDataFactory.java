package gps;

import java.util.Arrays;

public final class SampleDataFactory {
    private SampleDataFactory() {
    }

    public static GpsSimulator createCampusShuttleSimulator() {
        BusRoute route = new BusRoute(
                "Campus Shuttle Blue",
                Arrays.asList(
                        new BusStop("Main Gate", 0.0),
                        new BusStop("Faculty of Science", 0.8),
                        new BusStop("Library", 1.6),
                        new BusStop("Student Centre", 2.3),
                        new BusStop("Hostel", 3.1)));

        SimulatedBus bus = new SimulatedBus("T818", route, 24.0);
        return new GpsSimulator(bus);
    }
}
