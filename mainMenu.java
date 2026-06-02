import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;

import etacalculator.BusArrival;
import etacalculator.MultiBusSystem;
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
        tabs.addTab("Bus Map",       buildMapTab());
        tabs.addTab("Arrival Board", buildArrivalTab(tabs));
        tabs.addTab("Schedule",      buildScheduleTab());
        tabs.addTab("Bus Tracker",   buildTrackerTab(tabs));
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

    // ═══════════════════════════════════════════════════════
    // TAB 1 — Bus Map
    // ═══════════════════════════════════════════════════════

    private static JPanel buildMapTab() {
        JPanel wrapper = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Campus Bus Network Map", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(10, 8, 6, 8));
        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(new BusMapWindow().buildPanel(), BorderLayout.CENTER);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════
    // TAB 2 — Arrival Board
    // FIX: timer now checks isRunning() before start() so it
    //      loops continuously instead of restarting each tab switch
    // ═══════════════════════════════════════════════════════

    private static JPanel buildArrivalTab(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        MultiBusSystem system = new MultiBusSystem();
        try {
            for (SimulatedBus bus : RouteLoader.load().buses)
                system.addBus(new GpsSimulator(bus));
        } catch (Exception ex) {
            panel.add(new JLabel("Could not load bus data: " + ex.getMessage()), BorderLayout.CENTER);
            return panel;
        }

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JTextField searchField = new JTextField(22);
        JButton searchBtn = new JButton("Search Stop");
        JButton clearBtn  = new JButton("Clear");
        searchBar.add(new JLabel("Search stop:")); searchBar.add(searchField);
        searchBar.add(searchBtn); searchBar.add(clearBtn);

        JPanel infoRow = new JPanel(new GridLayout(1, 2, 16, 0));
        JLabel busCountLbl = new JLabel("Total Buses: " + system.getNumberOfBuses());
        JLabel nextLbl = new JLabel();
        busCountLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nextLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoRow.add(busCountLbl); infoRow.add(nextLbl);

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.add(searchBar, BorderLayout.NORTH);
        topSection.add(infoRow,   BorderLayout.SOUTH);
        panel.add(topSection, BorderLayout.NORTH);

        String[] cols = {"Bus ID","Route","Current Stop","Next Stop","Distance (km)","ETA (min)","Route ETA (min)"};
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
                ? system.getBusesForStop(searchQuery[0])
                : system.getBusesInArrivalOrder();
            model.setColumnIdentifiers(searching
                ? new String[]{"Bus ID","Route","Current Stop","Target Stop","Distance (km)","ETA (min)","Route ETA (min)"}
                : cols);
            for (BusArrival a : arrivals) {
                model.addRow(new Object[]{
                    a.getBusId(), a.getRouteName(), a.getCurrentStop(), a.getNextStop(),
                    DIST_FMT.format(a.getDistanceToNextStopKm()),
                    a.getEtaToNextStopMin(), a.getEtaToRouteEndMin()
                });
            }
            if (searching) {
                busCountLbl.setText("Buses approaching '" + searchQuery[0] + "': " + arrivals.size());
                nextLbl.setText(arrivals.isEmpty() ? "No buses found"
                    : "Next: Bus " + arrivals.get(0).getBusId() + " in " + arrivals.get(0).getEtaToNextStopMin() + " min");
            } else {
                busCountLbl.setText("Total Buses: " + system.getNumberOfBuses());
                BusArrival next = system.getNextArrival();
                nextLbl.setText(next != null && !next.isTripCompleted()
                    ? "Next Arrival: Bus " + next.getBusId() + " \u2192 " + next.getNextStop() + " in " + next.getEtaToNextStopMin() + " min"
                    : "All buses completed trips");
            }
        };

        // FIX: check isRunning() so timer doesn't restart on every tab switch
        Timer timer = new Timer(1000, e -> { system.tickAll(20); refresh.run(); });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == panel) {
                if (!timer.isRunning()) timer.start();
            } else {
                timer.stop();
            }
        });

        searchBtn.addActionListener(e -> { searchQuery[0] = searchField.getText().trim(); refresh.run(); });
        clearBtn.addActionListener(e  -> { searchField.setText(""); searchQuery[0] = ""; refresh.run(); });
        refresh.run();
        return panel;
    }

    // ═══════════════════════════════════════════════════════
    // TAB 3 — Schedule
    // ═══════════════════════════════════════════════════════

    private static JPanel buildScheduleTab() {
        ScheduleWindow sw = new ScheduleWindow();
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Timetables"));
        JLabel instruction = new JLabel("Enter Route / Bus ID (e.g. T818, Campus Shuttle Blue) or Stop Name:");
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JTextField searchField = new JTextField();
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton routeBtn = new JButton("Search Route / Bus");
        JButton stopBtn  = new JButton("Search Stop");
        btnRow.add(routeBtn); btnRow.add(stopBtn);
        searchPanel.add(instruction); searchPanel.add(searchField); searchPanel.add(btnRow);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Search Results"));

        JPanel addPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Custom Entry"));
        JTextField inRoute = new JTextField(), inId = new JTextField(),
                   inStop  = new JTextField(), inTime = new JTextField();
        JButton addBtn = new JButton("Add Entry");
        addPanel.add(new JLabel("Route Name:"));         addPanel.add(inRoute);
        addPanel.add(new JLabel("Bus ID:"));             addPanel.add(inId);
        addPanel.add(new JLabel("Stop Name:"));          addPanel.add(inStop);
        addPanel.add(new JLabel("Time (e.g. 11:00 AM):")); addPanel.add(inTime);
        addPanel.add(new JLabel(""));                    addPanel.add(addBtn);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scroll,      BorderLayout.CENTER);
        panel.add(addPanel,    BorderLayout.SOUTH);

        routeBtn.addActionListener(e -> resultArea.setText(sw.searchRoute(searchField.getText().trim())));
        stopBtn.addActionListener(e  -> resultArea.setText(sw.searchStop(searchField.getText().trim())));
        addBtn.addActionListener(e -> {
            String r = inRoute.getText().trim(), b = inId.getText().trim(),
                   s = inStop.getText().trim(),  t = inTime.getText().trim();
            if (r.isEmpty() || b.isEmpty() || s.isEmpty() || t.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please complete all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sw.addEntry(r, b, s, t);
            JOptionPane.showMessageDialog(panel, "Entry added! Search by route or stop name to verify.", "Success", JOptionPane.INFORMATION_MESSAGE);
            inRoute.setText(""); inId.setText(""); inStop.setText(""); inTime.setText("");
        });
        return panel;
    }

    // ═══════════════════════════════════════════════════════
    // TAB 4 — Bus Tracker
    // Enhanced: animated route progress strip shows bus icon
    // moving along the route line in real time.
    // FIX: isRunning() check prevents timer restart on tab switch.
    // ═══════════════════════════════════════════════════════

    private static JPanel buildTrackerTab(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Load buses
        List<SimulatedBus> buses;
        try {
            buses = RouteLoader.load().buses;
        } catch (Exception ex) {
            panel.add(new JLabel("Could not load bus data: " + ex.getMessage()), BorderLayout.CENTER);
            return panel;
        }

        // ── Bus picker ────────────────────────────────────────
        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel pickerLbl = new JLabel("Select bus:");
        pickerLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String[] options = buses.stream()
            .map(b -> b.getBusId() + " \u2014 " + b.getRoute().getRouteName())
            .toArray(String[]::new);
        JComboBox<String> picker = new JComboBox<>(options);
        picker.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pickerRow.add(pickerLbl); pickerRow.add(picker);
        panel.add(pickerRow, BorderLayout.NORTH);

        // ── Info labels (left column) ─────────────────────────
        JPanel dataPanel = new JPanel(new GridLayout(8, 2, 10, 8));
        dataPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel routeVal   = new JLabel("—"); JLabel busVal     = new JLabel("—");
        JLabel curStopVal = new JLabel("—"); JLabel nxtStopVal = new JLabel("—");
        JLabel distVal    = new JLabel("—"); JLabel etaNextVal = new JLabel("—");
        JLabel etaEndVal  = new JLabel("—"); JLabel statusVal  = new JLabel("—");
        Font kf = new Font("SansSerif", Font.BOLD, 13);
        Font vf = new Font("SansSerif", Font.PLAIN, 13);
        String[] keys = {"Route","Bus ID","Current Stop","Next Stop",
                         "Distance to Next Stop","ETA to Next Stop","ETA to Final Stop","Status"};
        JLabel[] vals = {routeVal,busVal,curStopVal,nxtStopVal,distVal,etaNextVal,etaEndVal,statusVal};
        for (int i = 0; i < keys.length; i++) {
            JLabel k = new JLabel(keys[i] + ":"); k.setFont(kf); vals[i].setFont(vf);
            dataPanel.add(k); dataPanel.add(vals[i]);
        }

        // ── Route progress canvas ─────────────────────────────
        // Shows the route as a horizontal line with stop dots.
        // A bus icon (filled circle) moves along it in real time.
        RouteCanvas routeCanvas = new RouteCanvas();
        routeCanvas.setPreferredSize(new Dimension(600, 110));
        routeCanvas.setBorder(BorderFactory.createTitledBorder("Live Route Progress"));

        // ── Centre: info + canvas stacked ────────────────────
        JPanel centrePanel = new JPanel(new BorderLayout(0, 8));
        centrePanel.add(dataPanel,    BorderLayout.CENTER);
        centrePanel.add(routeCanvas,  BorderLayout.SOUTH);
        panel.add(centrePanel, BorderLayout.CENTER);

        // ── Simulator state ───────────────────────────────────
        GpsSimulator[] sim = {new GpsSimulator(buses.get(0))};

        Runnable updateAll = () -> {
            SimulatedBus bus  = sim[0].getBus();
            BusStatus st      = sim[0].getStatus();
            BusRoute route    = bus.getRoute();
            double traveled   = bus.getTraveledDistanceKm();
            double totalDist  = route.getTotalDistanceKm();

            // Update info labels
            routeVal.setText(st.getRouteName());
            busVal.setText(st.getBusId());
            curStopVal.setText(st.getCurrentStop());
            nxtStopVal.setText(st.getNextStop());
            distVal.setText(DIST_FMT.format(st.getDistanceToNextStopKm()) + " km");
            etaNextVal.setText(st.getEtaToNextStopMinutes() + " minute(s)");
            etaEndVal.setText(st.getEtaToRouteEndMinutes() + " minute(s)");
            statusVal.setText(st.isTripCompleted()
                ? "Trip completed"
                : "Bus " + st.getBusId() + " will arrive at " + st.getNextStop()
                  + " in " + st.getEtaToNextStopMinutes() + " minute(s)");

            // Update canvas
            float progress = totalDist > 0 ? (float)(traveled / totalDist) : 0f;
            routeCanvas.update(route, progress, st.getBusId());
        };

        // FIX: isRunning() check — timer loops continuously, not one-shot
        Timer timer = new Timer(1000, e -> { sim[0].tick(20); updateAll.run(); });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == panel) {
                if (!timer.isRunning()) timer.start();
            } else {
                timer.stop();
            }
        });

        // Switching bus — reset simulator
        picker.addActionListener(e -> {
            int idx = picker.getSelectedIndex();
            if (idx < 0 || idx >= buses.size()) return;
            timer.stop();
            sim[0] = new GpsSimulator(buses.get(idx));
            updateAll.run();
            if (tabs.getSelectedComponent() == panel) timer.start();
        });

        updateAll.run();
        return panel;
    }

    // ═══════════════════════════════════════════════════════
    // RouteCanvas — animated route progress strip
    // ═══════════════════════════════════════════════════════

    /**
     * Custom JPanel that draws the bus route as a horizontal strip:
     *   - A grey track line from start to end
     *   - Green filled track showing how far the bus has travelled
     *   - White circles for each stop, with the stop name below
     *   - A moving bus icon (orange filled circle) at the bus's current position
     */
    static class RouteCanvas extends JPanel {

        private BusRoute route;
        private float    progress; // 0.0 = start, 1.0 = end
        private String   busId;

        private static final Color TRACK_BG   = new Color(0xDDDDDD);
        private static final Color TRACK_DONE = new Color(0x1D9E75);
        private static final Color STOP_CLR   = Color.WHITE;
        private static final Color STOP_BDR   = new Color(0x555555);
        private static final Color BUS_CLR    = new Color(0xFF8C00);   // orange
        private static final Color BUS_SHADOW = new Color(0xFF, 0x8C, 0x00, 80);

        public void update(BusRoute r, float prog, String id) {
            this.route    = r;
            this.progress = prog;
            this.busId    = id;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (route == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int PAD  = 40;          // left/right padding
            int trackY = h / 2 - 8; // vertical centre of track
            int trackH = 8;         // track thickness
            int trackW = w - PAD * 2;

            double totalDist = route.getTotalDistanceKm();

            // ── Background track ──────────────────────────────
            g2.setColor(TRACK_BG);
            g2.fill(new RoundRectangle2D.Float(PAD, trackY, trackW, trackH, trackH, trackH));

            // ── Progress track (green, shows how far bus has gone) ──
            int doneW = (int)(trackW * progress);
            if (doneW > 0) {
                g2.setColor(TRACK_DONE);
                g2.fill(new RoundRectangle2D.Float(PAD, trackY, doneW, trackH, trackH, trackH));
            }

            // ── Stop circles ──────────────────────────────────
            List<BusStop> stops = route.getStops();
            for (BusStop stop : stops) {
                double ratio = totalDist > 0 ? stop.getDistanceFromStartKm() / totalDist : 0;
                int sx = PAD + (int)(trackW * ratio);
                int sy = trackY + trackH / 2;

                // Circle
                int r = 7;
                g2.setColor(STOP_CLR);
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                g2.setColor(STOP_BDR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(sx - r, sy - r, r * 2, r * 2);

                // Stop name below
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                FontMetrics fm = g2.getFontMetrics();
                // Wrap long names onto two lines at spaces
                String name = stop.getName();
                String[] words = name.split(" ");
                if (words.length > 2) {
                    // Split roughly in half
                    int mid = words.length / 2;
                    StringBuilder line1 = new StringBuilder();
                    StringBuilder line2 = new StringBuilder();
                    for (int i = 0; i < words.length; i++) {
                        if (i < mid) { if (line1.length() > 0) line1.append(" "); line1.append(words[i]); }
                        else         { if (line2.length() > 0) line2.append(" "); line2.append(words[i]); }
                    }
                    g2.setColor(new Color(0x333333));
                    g2.drawString(line1.toString(), sx - fm.stringWidth(line1.toString()) / 2, sy + r + 12);
                    g2.drawString(line2.toString(), sx - fm.stringWidth(line2.toString()) / 2, sy + r + 22);
                } else {
                    g2.setColor(new Color(0x333333));
                    g2.drawString(name, sx - fm.stringWidth(name) / 2, sy + r + 12);
                }
            }

            // ── Moving bus icon ───────────────────────────────
            int bx = PAD + (int)(trackW * progress);
            int by = trackY + trackH / 2;
            int br = 10; // bus icon radius

            // Glow effect
            g2.setColor(BUS_SHADOW);
            g2.fillOval(bx - br - 4, by - br - 4, (br + 4) * 2, (br + 4) * 2);

            // Bus circle
            g2.setColor(BUS_CLR);
            g2.fillOval(bx - br, by - br, br * 2, br * 2);
            g2.setColor(BUS_CLR.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(bx - br, by - br, br * 2, br * 2);

            // Bus ID label inside the circle
            if (busId != null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                FontMetrics fm = g2.getFontMetrics();
                String label = busId.length() > 4 ? busId.substring(0, 4) : busId;
                g2.drawString(label, bx - fm.stringWidth(label) / 2, by + fm.getAscent() / 2 - 1);
            }
        }
    }
}