package gps;

public class BusStop {
    private final String name;
    private final double distanceFromStartKm;

    public BusStop(String name, double distanceFromStartKm) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stop name cannot be empty.");
        }
        if (distanceFromStartKm < 0) {
            throw new IllegalArgumentException("Stop distance cannot be negative.");
        }
        this.name = name;
        this.distanceFromStartKm = distanceFromStartKm;
    }

    public String getName() {
        return name;
    }

    public double getDistanceFromStartKm() {
        return distanceFromStartKm;
    }
}
