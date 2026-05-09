package gps;

public class BusStatus {
    private final String busId;
    private final String routeName;
    private final String currentStop;
    private final String nextStop;
    private final double distanceToNextStopKm;
    private final double distanceToRouteEndKm;
    private final int etaToNextStopMinutes;
    private final int etaToRouteEndMinutes;
    private final boolean tripCompleted;

    public BusStatus(
            String busId,
            String routeName,
            String currentStop,
            String nextStop,
            double distanceToNextStopKm,
            double distanceToRouteEndKm,
            int etaToNextStopMinutes,
            int etaToRouteEndMinutes,
            boolean tripCompleted) {
        this.busId = busId;
        this.routeName = routeName;
        this.currentStop = currentStop;
        this.nextStop = nextStop;
        this.distanceToNextStopKm = distanceToNextStopKm;
        this.distanceToRouteEndKm = distanceToRouteEndKm;
        this.etaToNextStopMinutes = etaToNextStopMinutes;
        this.etaToRouteEndMinutes = etaToRouteEndMinutes;
        this.tripCompleted = tripCompleted;
    }

    public String getBusId() {
        return busId;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getCurrentStop() {
        return currentStop;
    }

    public String getNextStop() {
        return nextStop;
    }

    public double getDistanceToNextStopKm() {
        return distanceToNextStopKm;
    }

    public double getDistanceToRouteEndKm() {
        return distanceToRouteEndKm;
    }

    public int getEtaToNextStopMinutes() {
        return etaToNextStopMinutes;
    }

    public int getEtaToRouteEndMinutes() {
        return etaToRouteEndMinutes;
    }

    public boolean isTripCompleted() {
        return tripCompleted;
    }
}
