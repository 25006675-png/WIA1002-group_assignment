package etacalculator;

import gps.BusRoute;
import gps.BusStatus;
import gps.BusStop;
import gps.GpsSimulator;
import gps.SimulatedBus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages multiple GpsSimulator instances and provides integration with MinHeap
 * for sorting buses by their ETA to next stop.

 * This system allows tracking of multiple buses and retrieving them in order
 * of lowest ETA, enabling features like real-time arrival boards.
 */
public class MultiBusSystem {
    private final List<GpsSimulator> simulators;
    private MinHeap arrivalHeap;

    public MultiBusSystem() {
        this.simulators = new ArrayList<>();
    }

    /**
     * Add a bus simulator to the system
     */
    public void addBus(GpsSimulator simulator) {
        if (simulator == null) {
            throw new IllegalArgumentException("simulator cannot be null");
        }
        simulators.add(simulator);
    }

    /**
     * Get all simulators
     */
    public List<GpsSimulator> getAllSimulators() {
        return new ArrayList<>(simulators);
    }

    /**
     * Get number of buses in the system
     */
    public int getNumberOfBuses() {
        return simulators.size();
    }

    /**
     * Advance all buses by the specified elapsed time
     */
    public void tickAll(int secondsElapsed) {
        for (GpsSimulator simulator : simulators) {
            simulator.tick(secondsElapsed);
        }
    }

    /**
     * Get status of a specific bus by index
     */
    public BusStatus getStatus(int index) {
        if (index < 0 || index >= simulators.size()) {
            throw new IndexOutOfBoundsException("Invalid bus index: " + index);
        }
        return simulators.get(index).getStatus();
    }

    /**
     * Get a MinHeap of all current bus arrivals sorted by ETA to next stop
     * Primary: ETA to next stop (ascending)
     * Secondary: ETA to route end (ascending)
     * Tertiary: Bus ID (lexicographical)
     */
    public MinHeap getArrivalHeap() {
        MinHeap heap = new MinHeap();
        
        for (GpsSimulator simulator : simulators) {
            BusStatus status = simulator.getStatus();
            BusArrival arrival = convertStatusToArrival(status);
            heap.insert(arrival);
        }
        
        return heap;
    }

    /**
     * Get all buses as BusArrival objects in sorted order (by ETA)
     * Returns a list of buses ordered by lowest ETA first
     */
    public List<BusArrival> getBusesInArrivalOrder() {
        MinHeap heap = getArrivalHeap();
        List<BusArrival> arrivals = new ArrayList<>();
        
        while (!heap.isEmpty()) {
            arrivals.add(heap.poll());
        }
        
        return arrivals;
    }

    /**
     * Peek at the bus arriving next (without removing from heap)
     */
    public BusArrival getNextArrival() {
        MinHeap heap = getArrivalHeap();
        return heap.peek();
    }

    /**
     * Find buses that will pass through a specific stop
     */
    public List<BusArrival> getBusesForStop(String targetStopName) {
        List<BusArrival> result = new ArrayList<>();
        for (GpsSimulator simulator : simulators) {
            SimulatedBus bus = simulator.getBus();
            BusRoute route = bus.getRoute();
            double traveled = bus.getTraveledDistanceKm();

            BusStop targetStop = null;
            for (BusStop stop : route.getStops()) {
                if (stop.getName().toLowerCase().contains(targetStopName.toLowerCase())) {
                    targetStop = stop;
                    break;
                }
            }

            if (targetStop != null && targetStop.getDistanceFromStartKm() >= traveled) {
                double distanceToTarget = targetStop.getDistanceFromStartKm() - traveled;
                int etaToTarget = toEtaMinutes(distanceToTarget, bus.getSpeedKmPerHour());

                BusStatus status = simulator.getStatus();

                result.add(new BusArrival(
                        bus.getBusId(),
                        route.getRouteName(),
                        status.getCurrentStop(),
                        targetStop.getName(),
                        distanceToTarget,
                        status.getDistanceToRouteEndKm(),
                        etaToTarget,
                        status.getEtaToRouteEndMinutes(),
                        false
                ));
            }
        }
        result.sort(Comparator.comparingInt(BusArrival::getEtaToNextStopMin));
        return result;
    }

    private int toEtaMinutes(double distanceKm, double speedKmPerHour) {
        double hours = distanceKm / speedKmPerHour;
        int minutes = (int) Math.ceil(hours * 60);
        return Math.max(0, minutes);
    }

    /**
     * Convert BusStatus to BusArrival
     */
    private BusArrival convertStatusToArrival(BusStatus status) {
        return new BusArrival(
            status.getBusId(),
            status.getRouteName(),
            status.getCurrentStop(),
            status.getNextStop(),
            status.getDistanceToNextStopKm(),
            status.getDistanceToRouteEndKm(),
            status.getEtaToNextStopMinutes(),
            status.getEtaToRouteEndMinutes(),
            status.isTripCompleted()
        );
    }
}