package etacalculator;

/**
 * Demonstration class for EtaCalculator and MinHeap priority queue system.
 * Shows how to:
 * - Calculate ETAs using EtaCalculator
 * - Create BusArrival objects
 * - Use MinHeap to sort buses by estimated arrival time
 * - Poll buses in ascending order of ETA
 *
 * Test case mapping:
 *   TC-U1 : 5.0 km at 60 km/h -> ETA = 5 min            (demonstrateEtaCalculation)
 *   TC-U2 : 0.1 km at 60 km/h -> ETA = 1 min (ceiling)  (demonstrateEtaCalculation)
 *   TC-U3 : 0.0 km             -> ETA = 0 min            (demonstrateEtaCalculation)
 *   TC-U4 : Insert 4 buses, poll in ascending ETA order   (demonstrateMinHeapPriorityQueue)
 *   TC-U5 : Tie-break by ETA to route end                 (demonstrateMinHeapPriorityQueue)
 */
public class EtaCalculatorTest {

    public static void main(String[] args) {
        System.out.println("--- ETA Calculator and Priority Queue Demo --- \n");

        // Demo 1: EtaCalculator static methods
        demonstrateEtaCalculation();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 2: MinHeap with BusArrival objects
        demonstrateMinHeapPriorityQueue();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 3: Real-world scenario
        demonstrateRealWorldScenario();
    }

    /**
     * Demo 1: Shows EtaCalculator usage for computing ETAs
     * Covers TC-U1, TC-U2, TC-U3
     */
    private static void demonstrateEtaCalculation() {
        System.out.println("DEMO 1: ETA Calculation\n");

        // Calculate ETA for various distances and speeds
        double distance1 = 5.0;   // km
        double speed1 = 60.0;     // km/h
        int eta1 = EtaCalculator.etaMinutes(distance1, speed1);

        double distance2 = 2.5;   // km
        double speed2 = 60.0;     // km/h
        int eta2 = EtaCalculator.etaMinutes(distance2, speed2);

        double distance3 = 10.0;  // km
        double speed3 = 40.0;     // km/h
        int eta3 = EtaCalculator.etaMinutes(distance3, speed3);

        System.out.println("Scenario: Calculate time to reach next bus stop");
        System.out.println("---");
        System.out.printf("Distance: %.1f km, Speed: %.0f km/h → ETA: %d minutes%n", 
            distance1, speed1, eta1);
        System.out.printf("Distance: %.1f km, Speed: %.0f km/h → ETA: %d minutes%n", 
            distance2, speed2, eta2);
        System.out.printf("Distance: %.1f km, Speed: %.0f km/h → ETA: %d minutes%n", 
            distance3, speed3, eta3);

        // TC-U3: Edge case: zero distance
        System.out.println("\n[TC-U3] Edge Case: Zero distance");
        int etaZero = EtaCalculator.etaMinutes(0.0, 60.0);
        System.out.printf("Distance: 0 km → ETA: %d minutes%n", etaZero);

        // TC-U2: Edge case: very short distance (rounds up)
        System.out.println("\n[TC-U2] Edge Case: Very short distance (rounds up)");
        int etaShort = EtaCalculator.etaMinutes(0.1, 60.0);
        System.out.printf("Distance: 0.1 km, Speed: 60 km/h → ETA: %d minute(s)%n", etaShort);

        // TC-U1: Standard calculation
        System.out.println("\n[TC-U1] Standard calculation");
        System.out.printf("Distance: %.1f km, Speed: %.0f km/h → ETA: %d minutes%n",
            distance1, speed1, eta1);
    }

    /**
     * Demo 2: Shows MinHeap usage with BusArrival objects
     * Covers TC-U4 (ascending poll order), TC-U5 (tie-break by route-end ETA)
     */
    private static void demonstrateMinHeapPriorityQueue() {
        System.out.println("DEMO 2: MinHeap Priority Queue\n");

        MinHeap heap = new MinHeap();

        // Create multiple BusArrival objects with different ETAs
        BusArrival bus1 = new BusArrival(
            "Bus-101",
            "RouteA",
            "Central Station",
            "Market Square",
            2.5,
            15.0,
            5,     // ETA to next stop: 5 min
            30,    // ETA to route end: 30 min
            false
        );

        BusArrival bus2 = new BusArrival(
            "Bus-202",
            "RouteB",
            "Park Avenue",
            "Library",
            1.0,
            8.0,
            2,     // ETA to next stop: 2 min (SOONEST)
            16,    // ETA to route end: 16 min
            false
        );

        BusArrival bus3 = new BusArrival("Bus-303", "RouteC", "Airport Road",
                "Terminal", 4.0, 20.0, 8,     // ETA to next stop: 8 min
            40, false);

        // TC-U5: Bus-104 has same ETA (2 min) as Bus-202, but shorter route-end ETA (10 < 16)
        //        -> Bus-104 should be polled before Bus-202
        BusArrival bus4 = new BusArrival("Bus-104", "RouteA", "Downtown",
            "Shopping Center", 1.5, 12.0, 2,     // ETA to next stop: 2 min (same as Bus-202, will use secondary sort)
            10, false);

        System.out.println("Inserting buses into MinHeap in random order:");
        System.out.println("---");

        // Insert in random order
        System.out.println("Insert: " + bus1.getBusId() + " (ETA: " + bus1.getEtaToNextStopMin() + " min)");
        heap.insert(bus1);

        System.out.println("Insert: " + bus2.getBusId() + " (ETA: " + bus2.getEtaToNextStopMin() + " min)");
        heap.insert(bus2);

        System.out.println("Insert: " + bus3.getBusId() + " (ETA: " + bus3.getEtaToNextStopMin() + " min)");
        heap.insert(bus3);

        System.out.println("Insert: " + bus4.getBusId() + " (ETA: " + bus4.getEtaToNextStopMin() + " min)");
        heap.insert(bus4);

        System.out.println("\nHeap size: " + heap.size());

        // Peek at the top
        System.out.println("\nPeek (view front without removing):");
        BusArrival peeked = heap.peek();
        System.out.println("  Next bus: " + peeked.getBusId() + 
            " (ETA: " + peeked.getEtaToNextStopMin() + " min) → " + peeked.getNextStop());
        System.out.println("  Heap size after peek: " + heap.size() + " (unchanged)");

        // TC-U4: Poll all buses in priority order — should be ascending ETA: 2, 2, 5, 8
        // TC-U5: First two polls should be Bus-104 then Bus-202 (tie-break by route-end ETA 10 < 16)
        System.out.println("\n[TC-U4] Polling buses in ascending order of ETA:");
        System.out.println("[TC-U5] Buses with same ETA sorted by route-end ETA (Bus-104 before Bus-202):");
        System.out.println("---");
        int priority = 1;
        while (!heap.isEmpty()) {
            BusArrival bus = heap.poll();
            System.out.printf("%d. Bus %s → %s (ETA: %d min, Route: %s)%n",
                priority++,
                bus.getBusId(),
                bus.getNextStop(),
                bus.getEtaToNextStopMin(),
                bus.getRouteName()
            );
        }

        System.out.println("\nHeap is now empty: " + heap.isEmpty());
    }

    /**
     * Demo 3: Real-world scenario - arrival board simulation
     */
    private static void demonstrateRealWorldScenario() {
        System.out.println("DEMO 3: Real-World Scenario - Bus Arrival Board\n");

        System.out.println("Scenario: Display incoming buses for Central Bus Station");
        System.out.println("---");

        MinHeap arrivalBoard = new MinHeap();

        // Simulate multiple buses arriving at different times
        String[][] busData = {
            {"X5", "Express Line", "Suburb", "Downtown", "3", "15", "6", "30"},
            {"A1", "Local Route", "Park", "Market", "1", "8", "2", "16"},
            {"B7", "Intercity", "Airport", "Station", "5", "25", "10", "50"},
            {"X5", "Express Line", "Suburb", "Downtown", "3", "15", "6", "30"},
            {"C3", "Night Bus", "Highway", "Center", "2", "10", "3", "20"},
        };

        for (String[] data : busData) {
            BusArrival arrival = new BusArrival(
                data[0],                           // busId
                data[1],                           // routeName
                data[2],                           // currentStop
                data[3],                           // nextStop
                Double.parseDouble(data[4]),       // distanceToNextStopKm
                Double.parseDouble(data[5]),       // distanceToRouteEndKm
                Integer.parseInt(data[6]),         // etaToNextStopMin
                Integer.parseInt(data[7]),         // etaToRouteEndMin
                false                              // tripCompleted
            );
            arrivalBoard.insert(arrival);
        }

        System.out.println("5 buses registered. Processing in arrival order:\n");
        System.out.println(String.format("%-10s %-15s %-15s %-12s %-8s", 
            "Bus ID", "Route", "Next Stop", "ETA (min)", "Distance"));
        System.out.println("-".repeat(70));

        int position = 1;
        while (!arrivalBoard.isEmpty()) {
            BusArrival bus = arrivalBoard.poll();
            System.out.printf("#%-8d %-15s %-15s %-12s %-8.1f km%n",
                position++,
                bus.getRouteName(),
                bus.getNextStop(),
                bus.getEtaToNextStopMin(),
                bus.getDistanceToNextStopKm()
            );
        }

        System.out.println("\n>>> All buses have arrived or left the queue <<<");
    }
}