package gps;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class GpsSimulator {
    private final SimulatedBus bus;
    private final Queue<BusStop> upcomingStops;
    private BusStop currentStop;

    public GpsSimulator(SimulatedBus bus) {
        if (bus == null) {
            throw new IllegalArgumentException("Bus cannot be null.");
        }
        this.bus = bus;
        this.currentStop = bus.getRoute().getStop(0);
        this.upcomingStops = new LinkedList<>(bus.getRoute().getStops());
        refreshUpcomingStops();
    }

    public void tick(int secondsElapsed) {

        bus.advance(secondsElapsed);
        refreshUpcomingStops();
    }

    public BusStatus getStatus() {
        BusRoute route = bus.getRoute();
        List<BusStop> stops = route.getStops();
        double traveled = bus.getTraveledDistanceKm();
        refreshUpcomingStops();

        if (bus.isTripCompleted()) {
            BusStop finalStop = stops.get(stops.size() - 1);
            return new BusStatus(
                    bus.getBusId(),
                    route.getRouteName(),
                    finalStop.getName(),
                    "No further stop",
                    0,
                    0,
                    0,
                    0,
                    true);
        }

        BusStop nextStop = upcomingStops.peek();

        double distanceToNextStop = nextStop.getDistanceFromStartKm() - traveled;
        double distanceToRouteEnd = route.getTotalDistanceKm() - traveled;

        return new BusStatus(
                bus.getBusId(),
                route.getRouteName(),
                currentStop.getName(),
                nextStop.getName(),
                distanceToNextStop,
                distanceToRouteEnd,
                toEtaMinutes(distanceToNextStop),
                toEtaMinutes(distanceToRouteEnd),
                false);
    }

    private void refreshUpcomingStops() {
        double traveled = bus.getTraveledDistanceKm();
        while (!upcomingStops.isEmpty()
                && upcomingStops.peek().getDistanceFromStartKm() <= traveled) {
            currentStop = upcomingStops.poll();
        }
    }

    private int toEtaMinutes(double distanceKm) {
        double hours = distanceKm / bus.getSpeedKmPerHour();
        int minutes = (int) Math.ceil(hours * 60);
        return Math.max(0, minutes);
    }

    public SimulatedBus getBus() {
        return bus;
    }

    public List<BusStop> getUpcomingStops() {
        refreshUpcomingStops();
        return new ArrayList<>(upcomingStops);
    }
}
