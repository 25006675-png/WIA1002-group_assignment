package schedulensearch;

import javax.swing.*;
import routedata.RouteLoader;
import java.awt.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule search system.
 * Changes from original:
 *  - initializeData() now loads from RouteLoader instead of hardcoded stops
 *  - Added public searchRoute(), searchStop(), addEntry() methods
 *    so MainMenu can call them without opening a separate JFrame
 *  - display() still works standalone if needed
 */
public class ScheduleWindow {
    private final HashMap<String, List<String>> routeMap;
    private final HashMap<String, List<String>> stopMap;

    public ScheduleWindow() {
        routeMap = new HashMap<>();
        stopMap  = new HashMap<>();
        initializeData();
    }

    private void initializeData() {
        try {
            RouteLoader.LoadResult result = RouteLoader.load();
            for (RouteLoader.RouteEntry route : result.routeEntries) {
                for (RouteLoader.StopEntry stop : route.stops) {
                    addScheduleEntry(route.routeName, route.busId, stop.name, stop.scheduledTime);
                }
            }
        } catch (Exception ex) {
            // Fallback to hardcoded data if RouteLoader fails
            addScheduleEntry("Campus Shuttle Blue", "T818", "Main Gate",          "08:00 AM");
            addScheduleEntry("Campus Shuttle Blue", "T818", "Faculty of Science", "08:10 AM");
            addScheduleEntry("Campus Shuttle Blue", "T818", "Library",            "08:20 AM");
            addScheduleEntry("Campus Shuttle Blue", "T818", "Student Centre",     "08:30 AM");
            addScheduleEntry("Campus Shuttle Blue", "T818", "Hostel Block A",     "08:40 AM");
        }
    }

    // ── Public API used by MainMenu tab ───────────────────────────────────────

    /** Search by route name or bus ID. Returns formatted result string. */
    public String searchRoute(String query) {
        String key = query.trim().toLowerCase();
        if (routeMap.containsKey(key)) {
            return "=== TIMETABLE FOR: " + query.toUpperCase() + " ===\n"
                + String.join("\n", routeMap.get(key));
        }
        return "No route or bus ID '" + query + "' found.";
    }

    /** Search by stop name. Returns formatted result string. */
    public String searchStop(String query) {
        String key = query.trim().toLowerCase();
        if (stopMap.containsKey(key)) {
            return "=== UPCOMING BUSES AT: " + query.toUpperCase() + " ===\n"
                + String.join("\n", stopMap.get(key));
        }
        return "No stop '" + query + "' found.";
    }

    /** Add a new schedule entry to both maps. */
    public void addEntry(String routeName, String busId, String stopName, String time) {
        addScheduleEntry(routeName, busId, stopName, time);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private void addScheduleEntry(String routeName, String busId, String stopName, String time) {
        String stopKey      = stopName.trim().toLowerCase();
        String routeNameKey = routeName.trim().toLowerCase();
        String busIdKey     = busId.trim().toLowerCase();
        String displayInfo  = time + " -> " + routeName + " [" + busId + "]";
        String routeDisplay = time + " - " + stopName;

        stopMap.computeIfAbsent(stopKey,      k -> new ArrayList<>()).add(displayInfo);
        routeMap.computeIfAbsent(routeNameKey, k -> new ArrayList<>()).add(routeDisplay);
        routeMap.computeIfAbsent(busIdKey,     k -> new ArrayList<>()).add(routeDisplay);
    }

    // ── Standalone display (kept for backward compatibility) ──────────────────

    public void display() {
        JFrame frame = new JFrame("Bus Schedule & Search System");
        frame.setSize(550, 500);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setLocationRelativeTo(null);

        JPanel searchPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Timetables"));
        JTextField searchField = new JTextField();
        JLabel instructionLabel = new JLabel("Enter Route/Bus ID (e.g., T818, Campus Shuttle Blue) or Stop Name:");
        JPanel searchBtnPanel = new JPanel(new FlowLayout());
        JButton routeBtn = new JButton("Search Route / Bus");
        JButton stopBtn  = new JButton("Search Stop");
        searchBtnPanel.add(routeBtn);
        searchBtnPanel.add(stopBtn);
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
        JTextField inId    = new JTextField();
        JTextField inStop  = new JTextField();
        JTextField inTime  = new JTextField();
        JButton addBtn = new JButton("Add Entry");
        adminPanel.add(new JLabel("Route Name:")); adminPanel.add(inRoute);
        adminPanel.add(new JLabel("Bus ID:"));     adminPanel.add(inId);
        adminPanel.add(new JLabel("Stop Name:"));  adminPanel.add(inStop);
        adminPanel.add(new JLabel("Time:"));       adminPanel.add(inTime);
        adminPanel.add(new JLabel(""));            adminPanel.add(addBtn);

        routeBtn.addActionListener(e -> resultArea.setText(searchRoute(searchField.getText())));
        stopBtn.addActionListener(e  -> resultArea.setText(searchStop(searchField.getText())));
        addBtn.addActionListener(e -> {
            String r = inRoute.getText().trim(), b = inId.getText().trim(),
                   s = inStop.getText().trim(),  t = inTime.getText().trim();
            if (r.isEmpty() || b.isEmpty() || s.isEmpty() || t.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please complete all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            addEntry(r, b, s, t);
            JOptionPane.showMessageDialog(frame, "Entry added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            inRoute.setText(""); inId.setText(""); inStop.setText(""); inTime.setText("");
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(searchPanel, BorderLayout.NORTH);
        frame.add(top,       BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(adminPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScheduleWindow().display());
    }
}