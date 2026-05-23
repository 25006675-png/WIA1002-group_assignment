package etacalculator;

import gps.BusRoute;
import gps.SimulatedBus;

/**
 * ETA calculation utilities in a standalone package.
 * This class mirrors the functionality previously placed under the `gps` package,
 * but is provided in its own package as requested.
 */
public final class EtaCalculator {

    private EtaCalculator() {
        // utility class - no instances
    }

    public static int etaMinutes(double distanceKm, double speedKmPerHour) {
        if (distanceKm < 0) {
            throw new IllegalArgumentException("distance(Km) cannot be negative");
        }
        if (distanceKm == 0) {
            return 0;
        }
        if (speedKmPerHour <= 0) {
            throw new IllegalArgumentException("Speed must be positive");
        }

        double hours = distanceKm / speedKmPerHour;
        int minutes = (int) Math.ceil(hours * 60.0);
        return Math.max(0, minutes);
    }

    public static int etaMinutes(double distanceKm, SimulatedBus bus) {
        if (bus == null) {
            throw new IllegalArgumentException("bus cannot be null");
        }
        return etaMinutes(distanceKm, bus.getSpeedKmPerHour());
    }

    public static int[] etasToAllStops(BusRoute route, SimulatedBus bus) {
        if (route == null || bus == null) {
            throw new IllegalArgumentException("route and bus cannot be null");
        }

        int n = route.size();
        int[] eta = new int[n];
        double traveled = bus.getTraveledDistanceKm();
        double speed = bus.getSpeedKmPerHour();

        for (int i = 0; i < n; i++) {
            double remaining = route.getStop(i).getDistanceFromStartKm() - traveled;
            if (remaining <= 0) {
                eta[i] = 0;
            } else {
                eta[i] = etaMinutes(remaining, speed);
            }
        }
        return eta;
    }
}

