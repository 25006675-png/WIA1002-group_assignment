package gps;

public class SimulatedBus {
    private final String busId;
    private final BusRoute route;
    private final double speedKmPerHour;
    private double traveledDistanceKm;

    public SimulatedBus(String busId, BusRoute route, double speedKmPerHour) {
        if (busId == null || busId.isBlank()) {
            throw new IllegalArgumentException("Bus ID cannot be empty.");
        }
        if (route == null) {
            throw new IllegalArgumentException("Route cannot be null.");
        }
        if (speedKmPerHour <= 0) {
            throw new IllegalArgumentException("Speed must be positive.");
        }

        this.busId = busId;
        this.route = route;
        this.speedKmPerHour = speedKmPerHour;
        this.traveledDistanceKm = 0;
    }

    public void advance(int secondsElapsed) {
        if (secondsElapsed < 0) {
            throw new IllegalArgumentException("Elapsed time cannot be negative.");
        }

        double distanceMoved = speedKmPerHour * (secondsElapsed / 3600.0);
        traveledDistanceKm = Math.min(route.getTotalDistanceKm(), traveledDistanceKm + distanceMoved);
    }

    public String getBusId() {
        return busId;
    }

    public BusRoute getRoute() {
        return route;
    }

    public double getSpeedKmPerHour() {
        return speedKmPerHour;
    }

    public double getTraveledDistanceKm() {
        return traveledDistanceKm;
    }

    public boolean isTripCompleted() {
        return traveledDistanceKm >= route.getTotalDistanceKm();
    }
}
