import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import etacalculator.BusArrival;
import gps.BusRoute;
import gps.BusStatus;
import gps.BusStop;
import gps.GpsSimulator;
import gps.SimulatedBus;
import routedata.BusMapWindow;
import routedata.RouteLoader;
import schedulensearch.ScheduleWindow;

public class MainMenu {
    private static final DecimalFormat DIST_FMT = new DecimalFormat("0.00");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::buildAndShow);
    }

    private static void buildAndShow() {
        JFrame frame = new JFrame("Bus Route and Schedule Tracker");
        frame.setSize(920, 680);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        JLabel header = new JLabel("Bus Route and Schedule Tracker", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setBorder(new EmptyBorder(16, 8, 8, 8));
        frame.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("Bus Map", buildMapTab());
        tabs.addTab("Arrival Board", buildArrivalTab(tabs));
        tabs.addTab("Schedule", buildScheduleTab());
        tabs.addTab("Bus Tracker", buildTrackerTab(tabs));
        frame.add(tabs, BorderLayout.CENTER);

        JLabel credits = new JLabel(
            "Done by: Nurul Lukman | Tee Yun | Goh Xuan | Choong Lin | Omar Elfasakhany | Chong Kang",
            SwingConstants.CENTER);
        credits.setFont(new Font("SansSerif", Font.PLAIN, 11));
        credits.setForeground(Color.GRAY);
        credits.setBorder(new EmptyBorder(4, 8, 8, 8));
        frame.add(credits, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static JPanel buildMapTab() {
        JPanel wrapper = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Campus Bus Network Map", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(10, 8, 6, 8));
        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(new BusMapWindow().buildPanel(), BorderLayout.CENTER);
        return wrapper;
    }

    private static JPanel buildArrivalTab(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        List<DemoTripState> states;
        try {
            states = buildDemoTripStates(RouteLoader.load().routeEntries);
        } catch (Exception ex) {
            panel.add(new JLabel("Could not load bus data: " + ex.getMessage()), BorderLayout.CENTER);
            return panel;
        }

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JTextField searchField = new JTextField(22);
        JButton searchBtn = new JButton("Search Stop");
        JButton clearBtn = new JButton("Clear");
        searchBar.add(new JLabel("Search stop:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(clearBtn);

        JPanel infoRow = new JPanel(new GridLayout(1, 2, 16, 0));
        JLabel busCountLbl = new JLabel("Demo Time: " + RouteLoader.DEMO_TIME);
        JLabel nextLbl = new JLabel();
        busCountLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nextLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoRow.add(busCountLbl);
        infoRow.add(nextLbl);

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.add(searchBar, BorderLayout.NORTH);
        topSection.add(infoRow, BorderLayout.SOUTH);
        panel.add(topSection, BorderLayout.NORTH);

        String[] cols = {"Status", "Trip ID", "Route", "Current Stop", "Next Stop",
                "Distance (km)", "ETA (min)", "Route ETA (min)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        final String[] searchQuery = {""};
        Runnable refresh = () -> {
            model.setRowCount(0);
            boolean searching = !searchQuery[0].isEmpty();
            List<BusArrival> arrivals = searching
                    ? getDemoArrivalsForStop(states, searchQuery[0])
                    : getDemoArrivals(states);

            model.setColumnIdentifiers(searching
                    ? new String[]{"Status", "Trip ID", "Route", "Current Stop", "Target Stop",
                            "Distance (km)", "ETA (min)", "Route ETA (min)"}
                    : cols);

            for (BusArrival a : arrivals) {
                model.addRow(new Object[]{
                    arrivalStatus(a),
                    a.getBusId(),
                    a.getRouteName(),
                    a.getCurrentStop(),
                    a.getNextStop(),
                    DIST_FMT.format(a.getDistanceToNextStopKm()),
                    a.getEtaToNextStopMin(),
                    a.getEtaToRouteEndMin()
                });
            }

            if (searching) {
                busCountLbl.setText("Buses approaching '" + searchQuery[0] + "': " + arrivals.size());
                nextLbl.setText(arrivals.isEmpty()
                        ? "No buses found"
                        : "Next: " + arrivals.get(0).getBusId() + " in "
                                + arrivals.get(0).getEtaToNextStopMin() + " min");
            } else {
                busCountLbl.setText("Demo Time: " + RouteLoader.DEMO_TIME + " | Routes: " + states.size());
                BusArrival next = arrivals.isEmpty() ? null : arrivals.get(0);
                nextLbl.setText(next == null
                        ? "No trips available"
                        : "Next: " + next.getBusId() + " -> " + next.getNextStop()
                                + " in " + next.getEtaToNextStopMin() + " min");
            }
        };

        Timer timer = new Timer(1000, e -> {
            for (DemoTripState state : states) state.tick(20);
            refresh.run();
        });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == panel) {
                if (!timer.isRunning()) timer.start();
            } else {
                timer.stop();
            }
        });

        searchBtn.addActionListener(e -> {
            searchQuery[0] = searchField.getText().trim();
            refresh.run();
        });
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            searchQuery[0] = "";
            refresh.run();
        });
        refresh.run();
        return panel;
    }

    private static JPanel buildScheduleTab() {
        ScheduleWindow sw = new ScheduleWindow();
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Timetables"));
        JLabel demo = new JLabel("Demo Time: " + RouteLoader.DEMO_TIME);
        JLabel instruction = new JLabel("Enter Route / Bus ID (e.g. T818, Campus Shuttle Blue) or Stop Name:");
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JTextField searchField = new JTextField();
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton routeBtn = new JButton("Search Route / Bus");
        JButton stopBtn = new JButton("Search Stop");
        btnRow.add(routeBtn);
        btnRow.add(stopBtn);
        searchPanel.add(demo);
        searchPanel.add(instruction);
        searchPanel.add(searchField);
        searchPanel.add(btnRow);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Search Results"));

        JPanel addPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Custom Entry"));
        JTextField inRoute = new JTextField(), inId = new JTextField();
        JTextField inStop = new JTextField(), inTime = new JTextField();
        JButton addBtn = new JButton("Add Entry");
        addPanel.add(new JLabel("Route Name:")); addPanel.add(inRoute);
        addPanel.add(new JLabel("Bus ID:")); addPanel.add(inId);
        addPanel.add(new JLabel("Stop Name:")); addPanel.add(inStop);
        addPanel.add(new JLabel("Time (e.g. 11:00 AM):")); addPanel.add(inTime);
        addPanel.add(new JLabel("")); addPanel.add(addBtn);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);

        routeBtn.addActionListener(e -> resultArea.setText(sw.searchRoute(searchField.getText().trim())));
        stopBtn.addActionListener(e -> resultArea.setText(sw.searchStop(searchField.getText().trim())));
        addBtn.addActionListener(e -> {
            String r = inRoute.getText().trim(), b = inId.getText().trim();
            String s = inStop.getText().trim(), t = inTime.getText().trim();
            if (r.isEmpty() || b.isEmpty() || s.isEmpty() || t.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please complete all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sw.addEntry(r, b, s, t);
            JOptionPane.showMessageDialog(panel, "Entry added! Search by route or stop name to verify.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            inRoute.setText(""); inId.setText(""); inStop.setText(""); inTime.setText("");
        });
        return panel;
    }

    private static JPanel buildTrackerTab(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(new EmptyBorder(10, 16, 10, 16));

        List<RouteLoader.RouteEntry> routes;
        try {
            routes = RouteLoader.load().routeEntries;
        } catch (Exception ex) {
            panel.add(new JLabel("Could not load bus data: " + ex.getMessage()), BorderLayout.CENTER);
            return panel;
        }

        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel pickerLbl = new JLabel("Select bus:");
        pickerLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String[] options = routes.stream()
                .map(r -> r.busId + " - " + r.routeName + " (" + RouteLoader.tripStatus(selectRelevantTrip(r)) + ")")
                .toArray(String[]::new);
        JComboBox<String> picker = new JComboBox<>(options);
        picker.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pickerRow.add(pickerLbl);
        pickerRow.add(picker);
        pickerRow.add(new JLabel("Demo Time: " + RouteLoader.DEMO_TIME));
        panel.add(pickerRow, BorderLayout.NORTH);

        JPanel dataPanel = new JPanel(new GridLayout(9, 2, 10, 8));
        dataPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel routeVal = new JLabel("-"), busVal = new JLabel("-");
        JLabel tripVal = new JLabel("-"), curStopVal = new JLabel("-");
        JLabel nxtStopVal = new JLabel("-"), distVal = new JLabel("-");
        JLabel etaNextVal = new JLabel("-"), etaEndVal = new JLabel("-");
        JLabel statusVal = new JLabel("-");

        Font kf = new Font("SansSerif", Font.BOLD, 13);
        Font vf = new Font("SansSerif", Font.PLAIN, 13);
        String[] keys = {"Route", "Bus ID", "Trip", "Current Stop", "Next Stop",
                "Distance to Next Stop", "ETA to Next Stop", "ETA to Final Stop", "Status"};
        JLabel[] vals = {routeVal, busVal, tripVal, curStopVal, nxtStopVal,
                distVal, etaNextVal, etaEndVal, statusVal};
        for (int i = 0; i < keys.length; i++) {
            JLabel k = new JLabel(keys[i] + ":");
            k.setFont(kf);
            vals[i].setFont(vf);
            dataPanel.add(k);
            dataPanel.add(vals[i]);
        }

        RouteCanvas routeCanvas = new RouteCanvas();
        routeCanvas.setPreferredSize(new Dimension(600, 110));
        routeCanvas.setBorder(BorderFactory.createTitledBorder("Live Route Progress"));

        JPanel centrePanel = new JPanel(new BorderLayout(0, 8));
        centrePanel.add(dataPanel, BorderLayout.CENTER);
        centrePanel.add(routeCanvas, BorderLayout.SOUTH);
        panel.add(centrePanel, BorderLayout.CENTER);

        DemoTripState[] state = {createDemoTripState(routes.get(0))};

        Runnable updateAll = () -> {
            DemoTripState current = state[0];
            BusRoute route = current.simulator.getBus().getRoute();
            routeVal.setText(current.route.routeName);
            busVal.setText(current.route.busId);
            tripVal.setText(current.trip.tripId + " (" + current.trip.startTime + " - " + current.trip.getEndTime() + ")");

            if (current.isUpcoming()) {
                curStopVal.setText("Not started");
                nxtStopVal.setText(current.route.stops.get(0).name);
                distVal.setText("-");
                etaNextVal.setText(RouteLoader.minutesUntil(current.trip.startTime) + " minute(s) until start");
                etaEndVal.setText((RouteLoader.minutesUntil(current.trip.startTime) + tripDurationMinutes(current.trip)) + " minute(s)");
                statusVal.setText("Upcoming - starts at " + current.trip.startTime);
                routeCanvas.update(route, 0f, current.route.busId);
                return;
            }

            BusStatus st = current.simulator.getStatus();
            double traveled = current.simulator.getBus().getTraveledDistanceKm();
            double totalDist = route.getTotalDistanceKm();
            curStopVal.setText(st.getCurrentStop());
            nxtStopVal.setText(st.getNextStop());
            distVal.setText(DIST_FMT.format(st.getDistanceToNextStopKm()) + " km");
            etaNextVal.setText(st.getEtaToNextStopMinutes() + " minute(s)");
            etaEndVal.setText(st.getEtaToRouteEndMinutes() + " minute(s)");
            statusVal.setText(st.isTripCompleted()
                    ? "Trip completed"
                    : "Active - arrives at " + st.getNextStop() + " in "
                            + st.getEtaToNextStopMinutes() + " minute(s)");

            float progress = totalDist > 0 ? (float) (traveled / totalDist) : 0f;
            routeCanvas.update(route, progress, current.route.busId);
        };

        Timer timer = new Timer(1000, e -> {
            state[0].tick(20);
            updateAll.run();
        });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == panel) {
                if (!timer.isRunning()) timer.start();
            } else {
                timer.stop();
            }
        });

        picker.addActionListener(e -> {
            int idx = picker.getSelectedIndex();
            if (idx < 0 || idx >= routes.size()) return;
            timer.stop();
            state[0] = createDemoTripState(routes.get(idx));
            updateAll.run();
            if (tabs.getSelectedComponent() == panel) timer.start();
        });

        updateAll.run();
        return panel;
    }

    private static List<DemoTripState> buildDemoTripStates(List<RouteLoader.RouteEntry> routes) {
        List<DemoTripState> states = new ArrayList<>();
        for (RouteLoader.RouteEntry route : routes) states.add(createDemoTripState(route));
        return states;
    }

    private static DemoTripState createDemoTripState(RouteLoader.RouteEntry route) {
        RouteLoader.TripEntry trip = selectRelevantTrip(route);
        SimulatedBus bus = RouteLoader.createSimulatedBus(route, trip.tripId);
        GpsSimulator simulator = new GpsSimulator(bus);
        String status = RouteLoader.tripStatus(trip);

        if (status.equals("Active")) {
            int elapsed = Math.max(0, RouteLoader.demoTimeMinutes() - RouteLoader.timeToMinutes(trip.startTime));
            simulator.tick(elapsed * 60);
        } else if (status.equals("Completed")) {
            simulator.tick(tripDurationMinutes(trip) * 60);
        }

        return new DemoTripState(route, trip, simulator, status);
    }

    private static RouteLoader.TripEntry selectRelevantTrip(RouteLoader.RouteEntry route) {
        RouteLoader.TripEntry last = route.trips.get(route.trips.size() - 1);
        for (RouteLoader.TripEntry trip : route.trips) {
            if (RouteLoader.tripStatus(trip).equals("Active")) return trip;
        }
        for (RouteLoader.TripEntry trip : route.trips) {
            if (RouteLoader.tripStatus(trip).equals("Upcoming")) return trip;
        }
        return last;
    }

    private static int tripDurationMinutes(RouteLoader.TripEntry trip) {
        return Math.max(0, RouteLoader.timeToMinutes(trip.getEndTime()) - RouteLoader.timeToMinutes(trip.startTime));
    }

    private static List<BusArrival> getDemoArrivals(List<DemoTripState> states) {
        List<BusArrival> arrivals = new ArrayList<>();
        for (DemoTripState state : states) arrivals.add(toArrival(state));
        arrivals.sort((a, b) -> Integer.compare(a.getEtaToNextStopMin(), b.getEtaToNextStopMin()));
        return arrivals;
    }

    private static List<BusArrival> getDemoArrivalsForStop(List<DemoTripState> states, String query) {
        List<BusArrival> arrivals = new ArrayList<>();
        String key = query.toLowerCase();
        for (DemoTripState state : states) {
            int targetIndex = -1;
            for (int i = 0; i < state.route.stops.size(); i++) {
                if (state.route.stops.get(i).name.toLowerCase().contains(key)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex < 0) continue;

            RouteLoader.StopEntry target = state.route.stops.get(targetIndex);
            if (state.isUpcoming()) {
                int eta = targetIndex < state.trip.stopTimes.size()
                        ? Math.max(0, RouteLoader.timeToMinutes(state.trip.stopTimes.get(targetIndex)) - RouteLoader.demoTimeMinutes())
                        : RouteLoader.minutesUntil(state.trip.startTime);
                arrivals.add(new BusArrival(state.trip.tripId, state.route.routeName, "Not started",
                        target.name, 0, state.simulator.getBus().getRoute().getTotalDistanceKm(),
                        eta, eta + tripDurationMinutes(state.trip), false));
                continue;
            }

            BusStatus status = state.simulator.getStatus();
            if (status.isTripCompleted()) continue;
            double traveled = state.simulator.getBus().getTraveledDistanceKm();
            if (target.distanceFromStartKm < traveled) continue;

            double distanceToTarget = target.distanceFromStartKm - traveled;
            int eta = etaMinutes(distanceToTarget, state.simulator.getBus().getSpeedKmPerHour());
            arrivals.add(new BusArrival(state.trip.tripId, state.route.routeName, status.getCurrentStop(),
                    target.name, distanceToTarget, status.getDistanceToRouteEndKm(),
                    eta, status.getEtaToRouteEndMinutes(), false));
        }
        arrivals.sort((a, b) -> Integer.compare(a.getEtaToNextStopMin(), b.getEtaToNextStopMin()));
        return arrivals;
    }

    private static BusArrival toArrival(DemoTripState state) {
        if (state.isUpcoming()) {
            int eta = RouteLoader.minutesUntil(state.trip.startTime);
            return new BusArrival(state.trip.tripId, state.route.routeName, "Not started",
                    state.route.stops.get(0).name, 0, state.simulator.getBus().getRoute().getTotalDistanceKm(),
                    eta, eta + tripDurationMinutes(state.trip), false);
        }
        BusStatus st = state.simulator.getStatus();
        return new BusArrival(state.trip.tripId, st.getRouteName(), st.getCurrentStop(), st.getNextStop(),
                st.getDistanceToNextStopKm(), st.getDistanceToRouteEndKm(),
                st.getEtaToNextStopMinutes(), st.getEtaToRouteEndMinutes(), st.isTripCompleted());
    }

    private static String arrivalStatus(BusArrival arrival) {
        if (arrival.getCurrentStop().equals("Not started")) return "Upcoming";
        if (arrival.isTripCompleted()) return "Completed";
        return "Active";
    }

    private static int etaMinutes(double distanceKm, double speedKmPerHour) {
        return Math.max(0, (int) Math.ceil((distanceKm / speedKmPerHour) * 60.0));
    }

    private static class DemoTripState {
        final RouteLoader.RouteEntry route;
        final RouteLoader.TripEntry trip;
        final GpsSimulator simulator;
        final String status;

        DemoTripState(RouteLoader.RouteEntry route, RouteLoader.TripEntry trip,
                      GpsSimulator simulator, String status) {
            this.route = route;
            this.trip = trip;
            this.simulator = simulator;
            this.status = status;
        }

        boolean isUpcoming() {
            return status.equals("Upcoming");
        }

        void tick(int seconds) {
            if (status.equals("Active")) simulator.tick(seconds);
        }
    }

    static class RouteCanvas extends JPanel {
        private BusRoute route;
        private float progress;
        private String busId;

        private static final Color TRACK_BG = new Color(0xDDDDDD);
        private static final Color TRACK_DONE = new Color(0x1D9E75);
        private static final Color STOP_CLR = Color.WHITE;
        private static final Color STOP_BDR = new Color(0x555555);
        private static final Color BUS_CLR = new Color(0xFF8C00);
        private static final Color BUS_SHADOW = new Color(0xFF, 0x8C, 0x00, 80);

        public void update(BusRoute r, float prog, String id) {
            this.route = r;
            this.progress = Math.max(0f, Math.min(1f, prog));
            this.busId = id;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (route == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 40;
            int trackY = h / 2 - 8;
            int trackH = 8;
            int trackW = w - pad * 2;
            double totalDist = route.getTotalDistanceKm();

            g2.setColor(TRACK_BG);
            g2.fill(new RoundRectangle2D.Float(pad, trackY, trackW, trackH, trackH, trackH));

            int doneW = (int) (trackW * progress);
            if (doneW > 0) {
                g2.setColor(TRACK_DONE);
                g2.fill(new RoundRectangle2D.Float(pad, trackY, doneW, trackH, trackH, trackH));
            }

            List<BusStop> stops = route.getStops();
            for (BusStop stop : stops) {
                double ratio = totalDist > 0 ? stop.getDistanceFromStartKm() / totalDist : 0;
                int sx = pad + (int) (trackW * ratio);
                int sy = trackY + trackH / 2;

                int r = 7;
                g2.setColor(STOP_CLR);
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                g2.setColor(STOP_BDR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(sx - r, sy - r, r * 2, r * 2);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(new Color(0x333333));
                String name = stop.getName();
                String[] words = name.split(" ");
                if (words.length > 2) {
                    int mid = words.length / 2;
                    String line1 = joinWords(words, 0, mid);
                    String line2 = joinWords(words, mid, words.length);
                    g2.drawString(line1, sx - fm.stringWidth(line1) / 2, sy + r + 12);
                    g2.drawString(line2, sx - fm.stringWidth(line2) / 2, sy + r + 22);
                } else {
                    g2.drawString(name, sx - fm.stringWidth(name) / 2, sy + r + 12);
                }
            }

            int bx = pad + (int) (trackW * progress);
            int by = trackY + trackH / 2;
            int br = 10;
            g2.setColor(BUS_SHADOW);
            g2.fillOval(bx - br - 4, by - br - 4, (br + 4) * 2, (br + 4) * 2);
            g2.setColor(BUS_CLR);
            g2.fillOval(bx - br, by - br, br * 2, br * 2);
            g2.setColor(BUS_CLR.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(bx - br, by - br, br * 2, br * 2);

            if (busId != null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                FontMetrics fm = g2.getFontMetrics();
                String label = busId.length() > 4 ? busId.substring(0, 4) : busId;
                g2.drawString(label, bx - fm.stringWidth(label) / 2, by + fm.getAscent() / 2 - 1);
            }
        }
    }

    private static String joinWords(String[] words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString();
    }
}
