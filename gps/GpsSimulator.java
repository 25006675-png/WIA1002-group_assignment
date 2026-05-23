package gps;

import java.util.List;


public class GpsSimulator {
    private final SimulatedBus bus;

    public GpsSimulator(SimulatedBus bus) {
        if (bus == null) {
            throw new IllegalArgumentException("Bus cannot be null.");
        }
        this.bus = bus;
    }

    public void tick(int secondsElapsed) {

        bus.advance(secondsElapsed);
    }

    public BusStatus getStatus() {
        BusRoute route = bus.getRoute();
        List<BusStop> stops = route.getStops();
        double traveled = bus.getTraveledDistanceKm();

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

        int nextStopIndex = findNextStopIndex(stops, traveled);
        int currentStopIndex = Math.max(0, nextStopIndex - 1);

        BusStop currentStop = stops.get(currentStopIndex);
        BusStop nextStop = stops.get(nextStopIndex);

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

    private int findNextStopIndex(List<BusStop> stops, double traveledDistanceKm) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getDistanceFromStartKm() > traveledDistanceKm) {
                return i;
            }
        }
        return stops.size() - 1;
    }

    private int toEtaMinutes(double distanceKm) {
        double hours = distanceKm / bus.getSpeedKmPerHour();
        int minutes = (int) Math.ceil(hours * 60);
        return Math.max(0, minutes);
    }

    public SimulatedBus getBus() {
        return bus;
    }
}
