package etacalculator;

import gps.GpsSimulator;
import gps.SampleDataFactory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * GUI for displaying a real-time multibus arrival board.
 * Shows all buses sorted by their ETA to the next stop (lowest ETA first).
 * Uses the MinHeap priority queue for automatic sorting.
 */
public class BusArrivalBoardDemo {
    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.00");
    private final MultiBusSystem multiBusSystem;
    private final DefaultTableModel tableModel;
    private final JLabel busCountLabel;
    private final JLabel nextArrivalLabel;
    private String searchStopQuery = "";

    public BusArrivalBoardDemo() {
        this.multiBusSystem = createMultiBusSystem();
        this.tableModel = new DefaultTableModel(
                new String[]{"Bus ID", "Route", "Current Stop", "Next Stop", "Distance (km)", "ETA (min)",
                    "Route ETA (min)"}, 0);
        this.busCountLabel = new JLabel();
        this.nextArrivalLabel = new JLabel();
    }

    /**
     * Create a multibus system with multiple sample buses
     */
    private MultiBusSystem createMultiBusSystem() {
        MultiBusSystem system = new MultiBusSystem();

        // Add multiple buses from sample data
        system.addBus(new GpsSimulator(SampleDataFactory.createCampusShuttle()));
        system.addBus(new GpsSimulator(SampleDataFactory.createExpress()));
        system.addBus(new GpsSimulator(SampleDataFactory.createLocalRoute()));

        return system;
    }

    public void show() {
        JFrame frame = new JFrame("Bus Arrival Board - Real-Time Multi-Bus Tracker");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLayout(new BorderLayout());

        // Top panel for title and search
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("Live Bus Arrival Board", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        topPanel.add(title, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search Stop");
        JButton clearButton = new JButton("Clear");
        
        searchPanel.add(new JLabel("Search Bus Stop: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);

        // Info panel (buses count and next arrival)
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 16, 8));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        infoPanel.add(busCountLabel);
        infoPanel.add(nextArrivalLabel);
        frame.add(infoPanel, BorderLayout.SOUTH);

        // Table for arrivals
        JTable arrivalTable = new JTable(tableModel);
        arrivalTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        arrivalTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        arrivalTable.setRowHeight(25);
        arrivalTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(arrivalTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Button listeners
        searchButton.addActionListener(e -> {
            searchStopQuery = searchField.getText();
            updateArrivalBoard();
        });
        clearButton.addActionListener(e -> {
            searchField.setText("");
            searchStopQuery = "";
            updateArrivalBoard();
        });

        // Update initial state
        updateArrivalBoard();

        // Timer to update every second
        Timer timer = new Timer(1000, event -> {
            multiBusSystem.tickAll(20);
            updateArrivalBoard();
        });
        timer.start();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Update the arrival board with buses sorted by ETA
     */
    private void updateArrivalBoard() {
        // Clear table
        tableModel.setRowCount(0);

        List<BusArrival> arrivals;
        boolean isSearching = searchStopQuery != null && !searchStopQuery.trim().isEmpty();

        if (isSearching) {
            arrivals = multiBusSystem.getBusesForStop(searchStopQuery.trim());
            tableModel.setColumnIdentifiers(new String[]{"Bus ID", "Route", "Current Stop", "Target Stop", "Distance (km)", "ETA (min)", "Route ETA (min)"});
        } else {
            arrivals = multiBusSystem.getBusesInArrivalOrder();
            tableModel.setColumnIdentifiers(new String[]{"Bus ID", "Route", "Current Stop", "Next Stop", "Distance (km)", "ETA (min)", "Route ETA (min)"});
        }

        // Add rows to table
        for (BusArrival arrival : arrivals) {
            tableModel.addRow(new Object[]{
                arrival.getBusId(),
                arrival.getRouteName(),
                arrival.getCurrentStop(),
                arrival.getNextStop(),
                DISTANCE_FORMAT.format(arrival.getDistanceToNextStopKm()),
                arrival.getEtaToNextStopMin(),
                arrival.getEtaToRouteEndMin()
            });
        }

        // Update info labels
        if (isSearching) {
            busCountLabel.setText("Buses approaching '" + searchStopQuery + "': " + arrivals.size());
            if (!arrivals.isEmpty()) {
                BusArrival next = arrivals.get(0);
                nextArrivalLabel.setText("Next Arrival: Bus " + next.getBusId() + " in " + next.getEtaToNextStopMin() + " min");
            } else {
                nextArrivalLabel.setText("Next Arrival: No buses found for this stop");
            }
        } else {
            busCountLabel.setText("Total Buses: " + multiBusSystem.getNumberOfBuses());
            BusArrival nextArrival = multiBusSystem.getNextArrival();
            if (nextArrival != null && !nextArrival.isTripCompleted()) {
                nextArrivalLabel.setText(
                    "Next Arrival: Bus " + nextArrival.getBusId()
                        + " → " + nextArrival.getNextStop()
                        + " in " + nextArrival.getEtaToNextStopMin() + " min"
                );
            } else {
                nextArrivalLabel.setText("Next Arrival: All buses completed trips");
            }
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new BusArrivalBoardDemo().show());
    }
}
