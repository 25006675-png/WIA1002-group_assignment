package schedulensearch;

import javax.swing.*;
import routedata.RouteLoader;
import java.awt.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule search system backed by routes.json.
 */
public class ScheduleWindow {
    private final HashMap<String, List<String>> routeMap;
    private final HashMap<String, List<String>> stopMap;

    public ScheduleWindow() {
        routeMap = new HashMap<>();
        stopMap = new HashMap<>();
        initializeData();
    }

    private void initializeData() {
        try {
            RouteLoader.LoadResult result = RouteLoader.load();
            for (RouteLoader.RouteEntry route : result.routeEntries) {
                for (RouteLoader.TripEntry trip : route.trips) {
                    String status = RouteLoader.tripStatus(trip);
                    int count = Math.min(route.stops.size(), trip.stopTimes.size());
                    for (int i = 0; i < count; i++) {
                        RouteLoader.StopEntry stop = route.stops.get(i);
                        addScheduleEntry(route.routeName, route.busId, trip.tripId,
                                stop.name, trip.stopTimes.get(i), status);
                    }
                }
            }
        } catch (Exception ex) {
            addEntry("Campus Shuttle Blue", "T818", "Main Gate", "08:00 AM");
            addEntry("Campus Shuttle Blue", "T818", "Faculty of Science", "08:05 AM");
            addEntry("Campus Shuttle Blue", "T818", "Library", "08:10 AM");
            addEntry("Campus Shuttle Blue", "T818", "Student Centre", "08:15 AM");
            addEntry("Campus Shuttle Blue", "T818", "Hostel Block A", "08:20 AM");
        }
    }

    public String searchRoute(String query) {
        String key = query.trim().toLowerCase();
        if (routeMap.containsKey(key)) {
            return "Demo Time: " + RouteLoader.DEMO_TIME + "\n"
                + "=== TIMETABLE FOR: " + query.toUpperCase() + " ===\n"
                + String.join("\n", routeMap.get(key));
        }
        return "No route or bus ID '" + query + "' found.";
    }

    public String searchStop(String query) {
        String key = query.trim().toLowerCase();
        if (stopMap.containsKey(key)) {
            return "Demo Time: " + RouteLoader.DEMO_TIME + "\n"
                + "=== UPCOMING BUSES AT: " + query.toUpperCase() + " ===\n"
                + String.join("\n", stopMap.get(key));
        }
        return "No stop '" + query + "' found.";
    }

    public void addEntry(String routeName, String busId, String stopName, String time) {
        addScheduleEntry(routeName, busId, busId + "-CUSTOM", stopName, time, "Custom");
    }

    private void addScheduleEntry(String routeName, String busId, String tripId,
                                  String stopName, String time, String status) {
        String stopKey = stopName.trim().toLowerCase();
        String routeNameKey = routeName.trim().toLowerCase();
        String busIdKey = busId.trim().toLowerCase();
        String tripIdKey = tripId.trim().toLowerCase();

        String prefix = "[" + status + "]";
        String routeDisplay = prefix + " " + tripId + " | " + time + " - " + stopName;
        String stopDisplay = prefix + " " + time + " -> " + routeName + " [" + tripId + "]";

        stopMap.computeIfAbsent(stopKey, k -> new ArrayList<>()).add(stopDisplay);
        routeMap.computeIfAbsent(routeNameKey, k -> new ArrayList<>()).add(routeDisplay);
        routeMap.computeIfAbsent(busIdKey, k -> new ArrayList<>()).add(routeDisplay);
        routeMap.computeIfAbsent(tripIdKey, k -> new ArrayList<>()).add(routeDisplay);
    }

    public void display() {
        JFrame frame = new JFrame("Bus Schedule & Search System");
        frame.setSize(550, 500);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setLocationRelativeTo(null);

        JPanel searchPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Timetables"));
        JLabel demoLabel = new JLabel("Demo Time: " + RouteLoader.DEMO_TIME);
        JTextField searchField = new JTextField();
        JLabel instructionLabel = new JLabel("Enter Route/Bus ID (e.g., T818, Campus Shuttle Blue) or Stop Name:");
        JPanel searchBtnPanel = new JPanel(new FlowLayout());
        JButton routeBtn = new JButton("Search Route / Bus");
        JButton stopBtn = new JButton("Search Stop");
        searchBtnPanel.add(routeBtn);
        searchBtnPanel.add(stopBtn);
        searchPanel.add(demoLabel);
        searchPanel.add(instructionLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtnPanel);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        JPanel adminPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        adminPanel.setBorder(BorderFactory.createTitledBorder("Add New Custom Entry"));
        JTextField inRoute = new JTextField();
        JTextField inId = new JTextField();
        JTextField inStop = new JTextField();
        JTextField inTime = new JTextField();
        JButton addBtn = new JButton("Add Entry");
        adminPanel.add(new JLabel("Route Name:")); adminPanel.add(inRoute);
        adminPanel.add(new JLabel("Bus ID:")); adminPanel.add(inId);
        adminPanel.add(new JLabel("Stop Name:")); adminPanel.add(inStop);
        adminPanel.add(new JLabel("Time:")); adminPanel.add(inTime);
        adminPanel.add(new JLabel("")); adminPanel.add(addBtn);

        routeBtn.addActionListener(e -> resultArea.setText(searchRoute(searchField.getText())));
        stopBtn.addActionListener(e -> resultArea.setText(searchStop(searchField.getText())));
        addBtn.addActionListener(e -> {
            String r = inRoute.getText().trim(), b = inId.getText().trim();
            String s = inStop.getText().trim(), t = inTime.getText().trim();
            if (r.isEmpty() || b.isEmpty() || s.isEmpty() || t.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please complete all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            addEntry(r, b, s, t);
            JOptionPane.showMessageDialog(frame, "Entry added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            inRoute.setText(""); inId.setText(""); inStop.setText(""); inTime.setText("");
        });

        frame.add(searchPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(adminPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScheduleWindow().display());
    }
}
