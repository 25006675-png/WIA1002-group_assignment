package etacalculator;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Priority queue for {@link BusArrival} objects.
 * Uses Java's built-in PriorityQueue with a custom comparator.
 * Orders arrivals by ETA to next stop (ascending), with tie-breakers for
 * stable ordering: ETA to route end, then busId lexicographically.
 */
public class MinHeap {
    private final PriorityQueue<BusArrival> pq;

    public MinHeap() {
        this(16);
    }

    public MinHeap(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        this.pq = new PriorityQueue<>(initialCapacity, createComparator());
    }

    public int size() {
        return pq.size();
    }

    public boolean isEmpty() {
        return pq.isEmpty();
    }

    public void clear() {
        pq.clear();
    }

    public void insert(BusArrival arrival) {
        if (arrival == null) {
            throw new IllegalArgumentException("arrival cannot be null");
        }
        pq.add(arrival);
    }

    public BusArrival peek() {
        return pq.peek();
    }

    public BusArrival poll() {
        return pq.poll();
    }

    /**
     * Creates a comparator that orders BusArrival objects by:
     * 1. ETA to next stop (ascending)
     * 2. ETA to route end (ascending, if primary is equal)
     * 3. Bus ID lexicographically (if secondary is equal)
     */
    private static Comparator<BusArrival> createComparator() {
        return (a, b) -> {
            if (a == b)
                return 0;
            // Primary: ETA to next stop
            int cmp = Integer.compare(a.getEtaToNextStopMin(), b.getEtaToNextStopMin());
            if (cmp != 0)
                return cmp;
            // Secondary: ETA to route end
            cmp = Integer.compare(a.getEtaToRouteEndMin(), b.getEtaToRouteEndMin());
            if (cmp != 0)
                return cmp;
            // Tertiary: Bus ID lexicographically
            String aid = a.getBusId();
            String bid = b.getBusId();
            if (aid == null && bid == null)
                return 0;
            if (aid == null)
                return -1;
            if (bid == null)
                return 1;
            return aid.compareTo(bid);
        };
    }
}

