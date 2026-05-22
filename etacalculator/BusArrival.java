package etacalculator;

public class BusArrival {
        private final String busId;
        private final String routeName;
        private final String currentStop;
        private final String nextStop;
        private final double distanceToNextStopKm;
        private final double distanceToRouteEndKm;
        private final int etaToNextStopMin;
        private final int etaToRouteEndMin;
        private final boolean tripCompleted;

        public BusArrival(String busId, String routeName, String currentStop, String nextStop,
                        double distanceToNextStopKm, double distanceToRouteEndKm,
                        int etaToNextStopMin, int etaToRouteEndMin, boolean tripCompleted) {
            this.busId = busId;
            this.routeName = routeName;
            this.currentStop = currentStop;
            this.nextStop = nextStop;
            this.distanceToNextStopKm = distanceToNextStopKm;
            this.distanceToRouteEndKm = distanceToRouteEndKm;
            this.etaToNextStopMin = etaToNextStopMin;
            this.etaToRouteEndMin = etaToRouteEndMin;
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

        public int getEtaToNextStopMin() {
            return etaToNextStopMin;
        }

        public int getEtaToRouteEndMin() {
            return etaToRouteEndMin;
        }

        public boolean isTripCompleted() {
            return tripCompleted;
        }
}
