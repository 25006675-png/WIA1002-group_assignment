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

    public static SimulatedBus createCampusShuttle() {
        BusRoute route = new BusRoute(
                "Campus Shuttle Blue",
                Arrays.asList(
                        new BusStop("Main Gate", 0.0),
                        new BusStop("Faculty of Science", 0.8),
                        new BusStop("Library", 1.6),
                        new BusStop("Student Centre", 2.3),
                        new BusStop("Hostel", 3.1)));
        return new SimulatedBus("T818", route, 24.0);
    }

    public static SimulatedBus createExpress() {
        BusRoute route = new BusRoute(
                "Express Route A",
                Arrays.asList(
                        new BusStop("Central Station", 0.0),
                        new BusStop("Market Square", 2.0),
                        new BusStop("Business District", 4.5),
                        new BusStop("Airport", 7.0)));
        return new SimulatedBus("X5", route, 40.0);
    }

    public static SimulatedBus createLocalRoute() {
        BusRoute route = new BusRoute(
                "Local Route C",
                Arrays.asList(
                        new BusStop("Downtown", 0.0),
                        new BusStop("Shopping Center", 1.2),
                        new BusStop("Hospital", 2.5),
                        new BusStop("Park", 3.8),
                        new BusStop("School", 5.0)));
        return new SimulatedBus("C3", route, 18.0);
    }
}
