package gps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BusRoute {
    private final String routeName;
    private final List<BusStop> stops;

    public BusRoute(String routeName, List<BusStop> stops) {
        if (routeName == null || routeName.isBlank()) {
            throw new IllegalArgumentException("Route name cannot be empty.");
        }
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("Route must contain at least two stops.");
        }

        validateStopDistances(stops);

        this.routeName = routeName;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    private void validateStopDistances(List<BusStop> stops) {
        double previousDistance = -1;
        for (BusStop stop : stops) {
            if (stop.getDistanceFromStartKm() < previousDistance) {
                throw new IllegalArgumentException("Stop distances must be in ascending order.");
            }
            previousDistance = stop.getDistanceFromStartKm();
        }
    }

    public String getRouteName() {
        return routeName;
    }

    public List<BusStop> getStops() {
        return stops;
    }

    public BusStop getStop(int index) {
        return stops.get(index);
    }

    public int size() {
        return stops.size();
    }

    public double getTotalDistanceKm() {
        return stops.get(stops.size() - 1).getDistanceFromStartKm();
    }
}
