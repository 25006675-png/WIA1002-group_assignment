package routedata;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bus Map window — draws the campus bus network as a schematic map.
 *
 * Fix applied: drawMap now receives Graphics2D directly from paintComponent
 * instead of calling canvas.getGraphics() which returns a disposable context
 * causing the map to disappear after the first paint.
 */
public class BusMapWindow {

    // ── Route colours ─────────────────────────────────────────────────────────
    private static final Map<String, Color> ROUTE_COLORS = new LinkedHashMap<>();
    static {
        ROUTE_COLORS.put("T818", new Color(0x1D9E75));
        ROUTE_COLORS.put("X5",   new Color(0x378ADD));
        ROUTE_COLORS.put("C3",   new Color(0xBA7517));
        ROUTE_COLORS.put("I1",   new Color(0xD4537E));
        ROUTE_COLORS.put("HUB",  new Color(0x7F77DD));
    }

    private static final Color SHARED_FILL   = new Color(0xE24B4A);
    private static final Color SHARED_BORDER = new Color(0xA32D2D);
    private static final Color STOP_FILL     = Color.WHITE;
    private static final Color ROAD_BG       = new Color(0xD0D8D0);
    private static final Color CAMPUS_BG     = new Color(0xE8F0E8);
    private static final Color SELECT_RING   = new Color(255, 220, 0, 100);

    // ── Stop positions (x, y on the canvas) ──────────────────────────────────
    private static final Map<String, int[]> STOP_POS = new LinkedHashMap<>();
    static {
        STOP_POS.put("Main Gate",              new int[]{80,  210});
        STOP_POS.put("Faculty of Science",     new int[]{190, 110});
        STOP_POS.put("Library",                new int[]{320, 110});
        STOP_POS.put("Student Centre",         new int[]{440, 210});
        STOP_POS.put("Hostel Block A",         new int[]{560, 110});
        STOP_POS.put("Faculty of Engineering", new int[]{560, 300});
        STOP_POS.put("Research Centre",        new int[]{650, 390});
        STOP_POS.put("Cafeteria",              new int[]{190, 310});
        STOP_POS.put("Medical Centre",         new int[]{320, 390});
        STOP_POS.put("Hostel Block B",         new int[]{440, 390});
        STOP_POS.put("Hostel Block C",         new int[]{560, 430});
        STOP_POS.put("Admin Block",            new int[]{320, 260});
        STOP_POS.put("Sports Complex",         new int[]{190, 390});
    }

    // ── Stop -> routes ────────────────────────────────────────────────────────
    private static final Map<String, List<String>> STOP_ROUTES = new LinkedHashMap<>();
    static {
        STOP_ROUTES.put("Main Gate",              Arrays.asList("T818","X5","C3","HUB"));
        STOP_ROUTES.put("Faculty of Science",     Arrays.asList("T818"));
        STOP_ROUTES.put("Library",                Arrays.asList("T818","I1","HUB"));
        STOP_ROUTES.put("Student Centre",         Arrays.asList("T818","X5","I1","HUB"));
        STOP_ROUTES.put("Hostel Block A",         Arrays.asList("T818"));
        STOP_ROUTES.put("Faculty of Engineering", Arrays.asList("X5"));
        STOP_ROUTES.put("Research Centre",        Arrays.asList("X5"));
        STOP_ROUTES.put("Cafeteria",              Arrays.asList("C3"));
        STOP_ROUTES.put("Medical Centre",         Arrays.asList("C3","I1","HUB"));
        STOP_ROUTES.put("Hostel Block B",         Arrays.asList("C3"));
        STOP_ROUTES.put("Hostel Block C",         Arrays.asList("C3"));
        STOP_ROUTES.put("Admin Block",            Arrays.asList("I1"));
        STOP_ROUTES.put("Sports Complex",         Arrays.asList("I1"));
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    private static final Object[][] EDGES = {
        {"Main Gate",              "Faculty of Science",     0.8, "T818", false},
        {"Faculty of Science",     "Library",                0.8, "T818", false},
        {"Library",                "Student Centre",         0.7, "T818", false},
        {"Student Centre",         "Hostel Block A",         0.8, "T818", false},
        {"Main Gate",              "Student Centre",         2.0, "X5",   false},
        {"Student Centre",         "Faculty of Engineering", 2.5, "X5",   false},
        {"Faculty of Engineering", "Research Centre",        2.5, "X5",   false},
        {"Main Gate",              "Cafeteria",              1.2, "C3",   false},
        {"Cafeteria",              "Medical Centre",         1.3, "C3",   false},
        {"Medical Centre",         "Hostel Block B",         1.3, "C3",   false},
        {"Hostel Block B",         "Hostel Block C",         1.2, "C3",   false},
        {"Student Centre",         "Library",                0.6, "I1",   false},
        {"Library",                "Admin Block",            0.8, "I1",   false},
        {"Admin Block",            "Medical Centre",         0.9, "I1",   false},
        {"Medical Centre",         "Sports Complex",         0.9, "I1",   false},
        {"Main Gate",              "Library",                1.5, "HUB",  true},
        {"Library",                "Student Centre",         1.5, "HUB",  true},
        {"Student Centre",         "Medical Centre",         1.5, "HUB",  true},
        {"Medical Centre",         "Main Gate",              1.5, "HUB",  true},
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private String activeRoute  = null;
    private String selectedStop = null;
    private JLabel infoBar;

    // ── Entry point ───────────────────────────────────────────────────────────

    public void show() {
        JFrame frame = new JFrame("Campus Bus Map");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(780, 640);
        frame.setLayout(new BorderLayout(0, 0));
        frame.setLocationRelativeTo(null);

        // Title
        JLabel title = new JLabel("Campus Bus Network Map", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setBorder(new EmptyBorder(14, 8, 6, 8));
        frame.add(title, BorderLayout.NORTH);

        // ── Map canvas ────────────────────────────────────────────────
        // FIX: pass Graphics2D from paintComponent directly into drawMap
        MapCanvas canvas = new MapCanvas();
        canvas.setPreferredSize(new Dimension(700, 460));
        canvas.setBackground(new Color(0xF4F7F4));
        canvas.setDrawCallback(g -> drawMap(g, canvas.getWidth(), canvas.getHeight()));
        frame.add(new JScrollPane(canvas), BorderLayout.CENTER);

        // ── South panel ───────────────────────────────────────────────
        JPanel southPanel = new JPanel(new BorderLayout(0, 4));
        southPanel.setBorder(new EmptyBorder(6, 12, 10, 12));

        // Route selector buttons
        JPanel routeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel filterLabel = new JLabel("Show route:");
        filterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        filterLabel.setForeground(Color.GRAY);
        routeBar.add(filterLabel);

        ButtonGroup group = new ButtonGroup();

        // All routes button
        JToggleButton allBtn = makeRouteButton("All routes", new Color(0x888780));
        allBtn.setSelected(true);
        group.add(allBtn);
        routeBar.add(allBtn);
        allBtn.addActionListener(e -> {
            activeRoute  = null;
            selectedStop = null;
            refreshInfoBar(null);
            canvas.repaint();
        });

        // Per-route buttons
        String[][] routeDefs = {
            {"T818", "T818 \u2014 Shuttle Blue"},
            {"X5",   "X5 \u2014 Shuttle Red"},
            {"C3",   "C3 \u2014 Shuttle Green"},
            {"I1",   "I1 \u2014 Inner Loop"},
            {"HUB",  "HUB \u2014 Connector"},
        };
        for (String[] def : routeDefs) {
            String rid   = def[0];
            String label = def[1];
            Color  color = ROUTE_COLORS.get(rid);
            JToggleButton btn = makeRouteButton(label, color);
            group.add(btn);
            routeBar.add(btn);
            btn.addActionListener(e -> {
                activeRoute  = rid;
                selectedStop = null;
                refreshInfoBar(null);
                canvas.repaint();
            });
        }

        southPanel.add(routeBar, BorderLayout.NORTH);

        // Info bar
        infoBar = new JLabel("Click any stop to see route information");
        infoBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoBar.setForeground(new Color(0x5F5E5A));
        infoBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xD3D1C7), 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        infoBar.setOpaque(true);
        infoBar.setBackground(new Color(0xF1EFE8));
        southPanel.add(infoBar, BorderLayout.CENTER);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legend.add(makeLegendItem("Regular stop",          new Color(0x1D9E75), false));
        legend.add(makeLegendItem("Shared / transfer stop", SHARED_FILL,        true));
        legend.add(makeLegendItem("HUB loop (dashed)",     new Color(0x7F77DD), false));
        southPanel.add(legend, BorderLayout.SOUTH);

        frame.add(southPanel, BorderLayout.SOUTH);

        // ── Stop click handler ────────────────────────────────────────
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String hit = hitTest(e.getX(), e.getY());
                selectedStop = hit;
                refreshInfoBar(hit);
                canvas.repaint();
            }
        });

        frame.setVisible(true);
    }

    // ── Core drawing — receives Graphics2D from paintComponent ────────────────

    private void drawMap(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Campus background
        g.setColor(new Color(0xF4F7F4));
        g.fillRect(0, 0, w, h);
        g.setColor(CAMPUS_BG);
        g.fill(new RoundRectangle2D.Float(30, 30, w - 60, h - 60, 16, 16));

        // Background road grid
        g.setColor(ROAD_BG);
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[][] roads = {
            {80, 30, 80, h - 30}, {320, 30, 320, h - 30}, {560, 30, 560, h - 30},
            {30, 210, w - 30, 210}, {30, 390, w - 30, 390}
        };
        for (int[] r : roads) g.drawLine(r[0], r[1], r[2], r[3]);

        // Buildings drawn at their actual stop positions
        // Each stop type gets a different shape:
        //   BUILDING  → filled rounded rect (academic/hostel buildings)
        //   GATE      → two small pillars (Main Gate)
        //   OUTDOOR   → filled circle patch (Sports Complex, open area)
        //   NONE      → no background shape (Research Centre — end of line)
        drawBuildings(g);

        // ── Draw edges ────────────────────────────────────────────────
        for (Object[] edge : EDGES) {
            String  from    = (String)  edge[0];
            String  to      = (String)  edge[1];
            double  dist    = (double)  edge[2];
            String  rid     = (String)  edge[3];
            boolean dashed  = (boolean) edge[4];

            int[] p1 = STOP_POS.get(from);
            int[] p2 = STOP_POS.get(to);
            if (p1 == null || p2 == null) continue;

            boolean visible = (activeRoute == null || activeRoute.equals(rid));
            float   alpha   = visible ? 0.9f : 0.08f;
            Color   c       = ROUTE_COLORS.getOrDefault(rid, Color.GRAY);

            g.setColor(applyAlpha(c, alpha));
            g.setStroke(dashed
                ? new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                  10, new float[]{10, 6}, 0)
                : new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(p1[0], p1[1], p2[0], p2[1]);

            // Distance label
            if (visible) {
                int mx = (p1[0] + p2[0]) / 2;
                int my = (p1[1] + p2[1]) / 2 - 8;
                g.setColor(new Color(80, 80, 80, 200));
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                FontMetrics fm = g.getFontMetrics();
                String label = dist + "km";
                g.drawString(label, mx - fm.stringWidth(label) / 2, my);
            }
        }

        // ── Draw stops ────────────────────────────────────────────────
        for (Map.Entry<String, int[]> entry : STOP_POS.entrySet()) {
            String       name   = entry.getKey();
            int[]        pos    = entry.getValue();
            List<String> routes = STOP_ROUTES.getOrDefault(name, Collections.emptyList());
            boolean shared  = routes.size() > 1;
            boolean visible = (activeRoute == null || routes.contains(activeRoute));
            int     r       = shared ? 12 : 9;
            float   alpha   = visible ? 1.0f : 0.15f;

            // Selection highlight ring
            if (name.equals(selectedStop)) {
                g.setColor(SELECT_RING);
                g.setStroke(new BasicStroke(1));
                g.fillOval(pos[0] - r - 6, pos[1] - r - 6, (r + 6) * 2, (r + 6) * 2);
            }

            // Circle fill
            g.setColor(applyAlpha(shared ? SHARED_FILL : STOP_FILL, alpha));
            g.fillOval(pos[0] - r, pos[1] - r, r * 2, r * 2);

            // Circle border
            Color border = shared ? SHARED_BORDER
                : ROUTE_COLORS.getOrDefault(routes.isEmpty() ? "" : routes.get(0), Color.GRAY);
            g.setColor(applyAlpha(border, alpha));
            g.setStroke(new BasicStroke(2));
            g.drawOval(pos[0] - r, pos[1] - r, r * 2, r * 2);

            // Inner coloured dot for regular stops
            if (!shared && !routes.isEmpty()) {
                g.setColor(applyAlpha(ROUTE_COLORS.getOrDefault(routes.get(0), Color.GRAY), alpha));
                g.fillOval(pos[0] - 4, pos[1] - 4, 8, 8);
            }

            // Stop name label
            g.setColor(applyAlpha(shared ? new Color(0x501313) : new Color(0x2C2C2A), alpha));
            g.setFont(new Font("SansSerif", shared ? Font.BOLD : Font.PLAIN, 10));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(name, pos[0] - fm.stringWidth(name) / 2, pos[1] + r + 13);
        }
    }

    // ── Buildings drawn at their stop positions ───────────────────────────────

    // Stop type definitions: name -> {width, height, type}
    // type: 0=building, 1=gate, 2=outdoor, 3=hostel, 4=none
    private static final Map<String, int[]> BUILDING_DEFS = new LinkedHashMap<>();
    static {
        //                                           w    h   type
        BUILDING_DEFS.put("Main Gate",              new int[]{60, 30, 1}); // gate pillars
        BUILDING_DEFS.put("Faculty of Science",     new int[]{80, 50, 0}); // academic block
        BUILDING_DEFS.put("Library",                new int[]{90, 55, 0}); // large building
        BUILDING_DEFS.put("Student Centre",         new int[]{85, 50, 0}); // large building
        BUILDING_DEFS.put("Hostel Block A",         new int[]{70, 45, 3}); // hostel
        BUILDING_DEFS.put("Faculty of Engineering", new int[]{80, 50, 0}); // academic block
        BUILDING_DEFS.put("Research Centre",        new int[]{75, 45, 0}); // academic block
        BUILDING_DEFS.put("Cafeteria",              new int[]{70, 40, 0}); // building
        BUILDING_DEFS.put("Medical Centre",         new int[]{80, 50, 0}); // building
        BUILDING_DEFS.put("Hostel Block B",         new int[]{70, 45, 3}); // hostel
        BUILDING_DEFS.put("Hostel Block C",         new int[]{70, 45, 3}); // hostel
        BUILDING_DEFS.put("Admin Block",            new int[]{75, 45, 0}); // building
        BUILDING_DEFS.put("Sports Complex",         new int[]{70, 70, 2}); // outdoor oval
    }

    private void drawBuildings(Graphics2D g) {
        for (Map.Entry<String, int[]> entry : BUILDING_DEFS.entrySet()) {
            String name = entry.getKey();
            int[]  def  = entry.getValue();
            int[]  pos  = STOP_POS.get(name);
            if (pos == null) continue;

            int bw   = def[0];
            int bh   = def[1];
            int type = def[2];
            int bx   = pos[0] - bw / 2;
            int by   = pos[1] - bh / 2;

            g.setStroke(new BasicStroke(1));
            switch (type) {
                case 0: // Academic / general building — pale rounded rect, border only
                    g.setColor(new Color(0xDDE8DD));
                    g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 6, 6));
                    g.setColor(new Color(0xBACCBA));
                    g.draw(new RoundRectangle2D.Float(bx, by, bw, bh, 6, 6));
                    break;

                case 1: // Gate — two thin pale pillars, no barrier decoration
                    g.setColor(new Color(0xCED8CE));
                    g.fill(new RoundRectangle2D.Float(bx,               by, 12, bh, 4, 4));
                    g.fill(new RoundRectangle2D.Float(bx + bw - 12, by, 12, bh, 4, 4));
                    g.setColor(new Color(0xADBDAD));
                    g.draw(new RoundRectangle2D.Float(bx,               by, 12, bh, 4, 4));
                    g.draw(new RoundRectangle2D.Float(bx + bw - 12, by, 12, bh, 4, 4));
                    break;

                case 2: // Outdoor — very pale green oval, no field lines
                    g.setColor(new Color(0xCCE8CC));
                    g.fillOval(bx, by, bw, bh);
                    g.setColor(new Color(0xAACAA));
                    g.drawOval(bx, by, bw, bh);
                    break;

                case 3: // Hostel — slightly different pale tone to distinguish from academic
                    g.setColor(new Color(0xDDE0E8));
                    g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 6, 6));
                    g.setColor(new Color(0xBBBECC));
                    g.draw(new RoundRectangle2D.Float(bx, by, bw, bh, 6, 6));
                    break;

                default:
                    break;
            }
        }
    }

    // ── Hit test ──────────────────────────────────────────────────────────────

    private String hitTest(int mx, int my) {
        String best     = null;
        double bestDist = 22;
        for (Map.Entry<String, int[]> e : STOP_POS.entrySet()) {
            int[]  p = e.getValue();
            double d = Math.hypot(mx - p[0], my - p[1]);
            if (d < bestDist) { bestDist = d; best = e.getKey(); }
        }
        return best;
    }

    // ── Info bar ──────────────────────────────────────────────────────────────

    private void refreshInfoBar(String stopName) {
        if (stopName == null) {
            infoBar.setForeground(new Color(0x5F5E5A));
            infoBar.setText(activeRoute == null
                ? "Click any stop to see route information"
                : "Showing: " + activeRoute + " \u2014 " + getRouteName(activeRoute)
                  + "   |   Click a stop for details");
            return;
        }
        List<String> routes = STOP_ROUTES.getOrDefault(stopName, Collections.emptyList());
        boolean shared = routes.size() > 1;
        String routeStr = String.join(", ", routes);
        infoBar.setText(stopName
            + (shared ? "   \u2605 Transfer point" : "")
            + "   \u2014   Served by: " + routeStr);
        infoBar.setForeground(shared ? SHARED_BORDER : new Color(0x5F5E5A));
    }

    private String getRouteName(String rid) {
        Map<String, String> names = new HashMap<>();
        names.put("T818", "Campus Shuttle Blue");
        names.put("X5",   "Campus Shuttle Red");
        names.put("C3",   "Campus Shuttle Green");
        names.put("I1",   "Inner Campus Loop");
        names.put("HUB",  "Hub Connector");
        return names.getOrDefault(rid, rid);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JToggleButton makeRouteButton(String label, Color color) {
        JToggleButton btn = new JToggleButton(label);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 1, true),
            new EmptyBorder(3, 8, 3, 8)
        ));
        Color lightBg = lighter(color);
        btn.setBackground(lightBg);
        btn.setForeground(color.darker());
        btn.setOpaque(true);
        btn.addChangeListener(e -> {
            btn.setBackground(btn.isSelected() ? color       : lightBg);
            btn.setForeground(btn.isSelected() ? Color.WHITE : color.darker());
        });
        return btn;
    }

    private JPanel makeLegendItem(String label, Color color, boolean filled) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(filled ? color : Color.WHITE);
                g2.fillOval(1, 1, 12, 12);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(1, 1, 12, 12);
            }
        };
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(Color.GRAY);
        p.add(dot); p.add(lbl);
        return p;
    }

    private Color lighter(Color c) {
        return new Color(
            Math.min(255, c.getRed()   + 160),
            Math.min(255, c.getGreen() + 160),
            Math.min(255, c.getBlue()  + 160)
        );
    }

    private Color applyAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 255));
    }

    // ── Custom canvas — Graphics2D flows from paintComponent into drawMap ─────

    static class MapCanvas extends JPanel {
        private Consumer<Graphics2D> drawCallback;

        public void setDrawCallback(Consumer<Graphics2D> cb) {
            this.drawCallback = cb;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (drawCallback != null) {
                drawCallback.accept((Graphics2D) g); // correct: same Graphics2D Swing owns
            }
        }
    }

    /**
     * Returns the map as an embeddable JPanel for use inside a JTabbedPane.
     * Contains the same canvas, route buttons, info bar and legend as show(),
     * but without creating a new JFrame.
     */
    public JPanel buildPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));

        MapCanvas canvas = new MapCanvas();
        canvas.setPreferredSize(new Dimension(700, 420));
        canvas.setBackground(new Color(0xF4F7F4));
        canvas.setDrawCallback(g -> drawMap(g, canvas.getWidth(), canvas.getHeight()));
        wrapper.add(new JScrollPane(canvas), BorderLayout.CENTER);

        // South: route buttons + info bar + legend
        JPanel southPanel = new JPanel(new BorderLayout(0, 4));
        southPanel.setBorder(new EmptyBorder(6, 12, 8, 12));

        JPanel routeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel filterLabel = new JLabel("Show route:");
        filterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        filterLabel.setForeground(Color.GRAY);
        routeBar.add(filterLabel);

        ButtonGroup group = new ButtonGroup();
        JToggleButton allBtn = makeRouteButton("All routes", new Color(0x888780));
        allBtn.setSelected(true);
        group.add(allBtn);
        routeBar.add(allBtn);
        allBtn.addActionListener(e -> {
            activeRoute = null; selectedStop = null;
            refreshInfoBar(null); canvas.repaint();
        });

        String[][] routeDefs = {
            {"T818","T818 \u2014 Shuttle Blue"},{"X5","X5 \u2014 Shuttle Red"},
            {"C3","C3 \u2014 Shuttle Green"},{"I1","I1 \u2014 Inner Loop"},{"HUB","HUB \u2014 Connector"},
        };
        for (String[] def : routeDefs) {
            String rid = def[0];
            JToggleButton btn = makeRouteButton(def[1], ROUTE_COLORS.get(rid));
            group.add(btn);
            routeBar.add(btn);
            btn.addActionListener(e -> {
                activeRoute = rid; selectedStop = null;
                refreshInfoBar(null); canvas.repaint();
            });
        }
        southPanel.add(routeBar, BorderLayout.NORTH);

        infoBar = new JLabel("Click any stop to see route information");
        infoBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoBar.setForeground(new Color(0x5F5E5A));
        infoBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xD3D1C7), 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        infoBar.setOpaque(true);
        infoBar.setBackground(new Color(0xF1EFE8));
        southPanel.add(infoBar, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legend.add(makeLegendItem("Regular stop",           new Color(0x1D9E75), false));
        legend.add(makeLegendItem("Shared / transfer stop", SHARED_FILL,         true));
        legend.add(makeLegendItem("HUB loop (dashed)",      new Color(0x7F77DD), false));
        southPanel.add(legend, BorderLayout.SOUTH);

        wrapper.add(southPanel, BorderLayout.SOUTH);

        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedStop = hitTest(e.getX(), e.getY());
                refreshInfoBar(selectedStop);
                canvas.repaint();
            }
        });

        return wrapper;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BusMapWindow().show());
    }
}