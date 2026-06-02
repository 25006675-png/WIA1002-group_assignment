package gps;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Live tracker window for a single bus.
 * Can run standalone (no-arg constructor uses T818 by default)
 * or be driven by MainMenu's tab (parameterised constructor).
 */
public class BusTrackerDemo {

    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.00");

    private final GpsSimulator simulator;
    private final JLabel routeValue;
    private final JLabel busValue;
    private final JLabel currentStopValue;
    private final JLabel nextStopValue;
    private final JLabel distanceValue;
    private final JLabel etaNextValue;
    private final JLabel etaFinalValue;
    private final JLabel statusValue;

    /** Standalone constructor — defaults to T818 Campus Shuttle. */
    public BusTrackerDemo() {
        this(SampleDataFactory.createCampusShuttleSimulator());
    }

    /** Parameterised constructor — used by MainMenu tracker tab. */
    public BusTrackerDemo(GpsSimulator simulator) {
        this.simulator        = simulator;
        this.routeValue       = new JLabel();
        this.busValue         = new JLabel();
        this.currentStopValue = new JLabel();
        this.nextStopValue    = new JLabel();
        this.distanceValue    = new JLabel();
        this.etaNextValue     = new JLabel();
        this.etaFinalValue    = new JLabel();
        this.statusValue      = new JLabel();
    }

    public void show() {
        JFrame frame = new JFrame("GPS Bus Tracker");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(520, 320);
        frame.setLayout(new BorderLayout());

        JLabel title = new JLabel("Simulated Live Bus Tracker", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        frame.add(title, BorderLayout.NORTH);

        JPanel dataPanel = new JPanel(new GridLayout(8, 2, 8, 8));
        dataPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        addRow(dataPanel, "Route",                 routeValue);
        addRow(dataPanel, "Bus ID",                busValue);
        addRow(dataPanel, "Current Stop",          currentStopValue);
        addRow(dataPanel, "Next Stop",             nextStopValue);
        addRow(dataPanel, "Distance to Next Stop", distanceValue);
        addRow(dataPanel, "ETA to Next Stop",      etaNextValue);
        addRow(dataPanel, "ETA to Final Stop",     etaFinalValue);
        addRow(dataPanel, "Status",                statusValue);
        frame.add(dataPanel, BorderLayout.CENTER);

        updateLabels();

        Timer timer = new Timer(1000, event -> {
            simulator.tick(20);
            updateLabels();
        });
        timer.start();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addRow(JPanel panel, String label, JLabel valueLabel) {
        JLabel keyLabel = new JLabel(label + ":");
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(keyLabel);
        panel.add(valueLabel);
    }

    private void updateLabels() {
        BusStatus status = simulator.getStatus();
        routeValue.setText(status.getRouteName());
        busValue.setText(status.getBusId());
        currentStopValue.setText(status.getCurrentStop());
        nextStopValue.setText(status.getNextStop());
        distanceValue.setText(DISTANCE_FORMAT.format(status.getDistanceToNextStopKm()) + " km");
        etaNextValue.setText(status.getEtaToNextStopMinutes() + " minute(s)");
        etaFinalValue.setText(status.getEtaToRouteEndMinutes() + " minute(s)");
        statusValue.setText(status.isTripCompleted()
            ? "Trip completed"
            : "Bus " + status.getBusId() + " will arrive at "
              + status.getNextStop() + " in " + status.getEtaToNextStopMinutes() + " minute(s)");
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new BusTrackerDemo().show());
    }
}