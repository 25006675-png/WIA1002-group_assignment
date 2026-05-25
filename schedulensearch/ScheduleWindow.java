package schedulensearch;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class ScheduleWindow {
    private final HashMap<String, List<String>> routeMap;
    private final HashMap<String, List<String>> stopMap;

    public ScheduleWindow() {
        routeMap = new HashMap<>();
        stopMap = new HashMap<>();
        initializeData();
    }

    private void addScheduleEntry(String routeName, String busId, String stopName, String time) {
        String stopKey = stopName.trim().toLowerCase();
        String displayInfo = time + " -> " + routeName + " [" + busId + "]";


        stopMap.computeIfAbsent(stopKey, k -> new ArrayList<>()).add(displayInfo);

        String routeNameKey = routeName.trim().toLowerCase();
        String busIdKey = busId.trim().toLowerCase();
        String routeDisplayInfo = time + " - " + stopName;

        routeMap.computeIfAbsent(routeNameKey, k -> new ArrayList<>()).add(routeDisplayInfo);
        routeMap.computeIfAbsent(busIdKey, k -> new ArrayList<>()).add(routeDisplayInfo);
    }

    private void initializeData() {
        //T818
        addScheduleEntry("Campus Shuttle Blue", "T818", "Main Gate", "08:00 AM");
        addScheduleEntry("Campus Shuttle Blue", "T818", "Faculty of Science", "08:10 AM");
        addScheduleEntry("Campus Shuttle Blue", "T818", "Library", "08:20 AM");
        addScheduleEntry("Campus Shuttle Blue", "T818", "Student Centre", "08:30 AM");
        addScheduleEntry("Campus Shuttle Blue", "T818", "Hostel", "08:40 AM");

        // X5
        addScheduleEntry("Express Route A", "X5", "Central Station", "09:00 AM");
        addScheduleEntry("Express Route A", "X5", "Market Square", "09:15 AM");
        addScheduleEntry("Express Route A", "X5", "Business District", "09:35 AM");
        addScheduleEntry("Express Route A", "X5", "Airport", "10:00 AM");

        //C3
        addScheduleEntry("Local Route C", "C3", "Downtown", "07:30 AM");
        addScheduleEntry("Local Route C", "C3", "Shopping Center", "07:45 AM");
        addScheduleEntry("Local Route C", "C3", "Hospital", "08:05 AM");
        addScheduleEntry("Local Route C", "C3", "Park", "08:20 AM");
        addScheduleEntry("Local Route C", "C3", "School", "08:40 AM");
    }

    public void display() {
        JFrame frame = new JFrame("Bus Schedule & Search System");
        frame.setSize(550, 500);
        frame.setLayout(new BorderLayout(10, 10));

        //SEARCH PANEL
        JPanel searchPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Timetables"));

        JTextField searchField = new JTextField();
        JLabel instructionLabel = new JLabel("Enter Route/Bus ID (e.g., T818, Express Route A) or Stop Name:");

        JPanel searchBtnPanel = new JPanel(new FlowLayout());
        JButton routeBtn = new JButton("Search Route / Bus");
        JButton stopBtn = new JButton("Search Stop");
        searchBtnPanel.add(routeBtn);
        searchBtnPanel.add(stopBtn);

        searchPanel.add(instructionLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtnPanel);

        //RESULT PANEL
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        //DATA ENTRY PANEL
        JPanel adminPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        adminPanel.setBorder(BorderFactory.createTitledBorder("Add New Custom Entry"));

        JTextField inputRoute = new JTextField();
        JTextField inputID = new JTextField();
        JTextField inputStop = new JTextField();
        JTextField inputTime = new JTextField();
        JButton addBtn = new JButton("Add Entry");

        adminPanel.add(new JLabel("Route Name:"));
        adminPanel.add(inputRoute);
        adminPanel.add(new JLabel("Bus ID:"));
        adminPanel.add(inputID);
        adminPanel.add(new JLabel("Stop Name:"));
        adminPanel.add(inputStop);
        adminPanel.add(new JLabel("Time (e.g., 11:00 AM):"));
        adminPanel.add(inputTime);
        adminPanel.add(new JLabel(""));
        adminPanel.add(addBtn);

       //HASHMAP OPERATION
        routeBtn.addActionListener(e -> {
            String query = searchField.getText().trim().toLowerCase();
            if (routeMap.containsKey(query)) {
                resultArea.setText("=== TIMETABLE FOR: " + searchField.getText().toUpperCase() + " ===\n" +
                        String.join("\n", routeMap.get(query)));
            } else {
                resultArea.setText("No Route or Bus ID '" + searchField.getText() + "' not found.");
            }
        });

        stopBtn.addActionListener(e -> {
            String query = searchField.getText().trim().toLowerCase();
            if (stopMap.containsKey(query)) {
                resultArea.setText("=== UPCOMING BUSES AT: " + searchField.getText().toUpperCase() + " ===\n" +
                        String.join("\n", stopMap.get(query)));
            } else {
                resultArea.setText("No Station Stop '" + searchField.getText() + "' not found.");
            }
        });

        addBtn.addActionListener(e -> {
            String rName = inputRoute.getText().trim();
            String bId = inputID.getText().trim();
            String sName = inputStop.getText().trim();
            String tVal = inputTime.getText().trim();

            if (rName.isEmpty() || bId.isEmpty() || sName.isEmpty() || tVal.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please complete all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            addScheduleEntry(rName, bId, sName, tVal);
            JOptionPane.showMessageDialog(frame, "Successfully saved data into structures!", "Success", JOptionPane.INFORMATION_MESSAGE);

            inputRoute.setText("");
            inputID.setText("");
            inputStop.setText("");
            inputTime.setText("");
        });

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(searchPanel, BorderLayout.NORTH);

        frame.add(topContainer, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(adminPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ScheduleWindow().display();
        });
    }
}