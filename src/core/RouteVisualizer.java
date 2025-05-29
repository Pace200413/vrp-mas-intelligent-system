package core;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RouteVisualizer extends JPanel {

    /* ---------- immutable data ---------- */
    private final Node depot;
    private final ArrayList<Route> routes;
    private final String summary;

    /* ---------- rendering state ---------- */
    private int SCALE;
    private BufferedImage truckImg;
    private Node   hoveredNode = null;
    private String hoverText   = null;

    /* animation */
    private final java.util.List<TruckState> truckStates = new ArrayList<>();
    private final Timer animationTimer;

    /* ---------- constants ---------- */
    private static final int NODE_RADIUS = 10;
    private static final int LEGEND_W    = 180;
    private static final int GAP_X       = 20;
    private static final int GAP_Y       = 20;
    private static final Color[] COLORS = {
            new Color(0xe6194b), new Color(0x3cb44b), new Color(0x0082c8),
            new Color(0xf58231), new Color(0x911eb4), new Color(0x46f0f0),
            new Color(0xf032e6), new Color(0xd2f53c), new Color(0xfabebe),
            new Color(0x008080), new Color(0xe6beff), new Color(0xaa6e28)
    };

    /* ---------- ctor ---------- */
    public RouteVisualizer(Node depot, ArrayList<Route> routes, String summary) {
        this.depot   = depot;
        this.routes  = routes;
        this.summary = summary;
        this.SCALE   = calcScale();

        setPreferredSize(new Dimension(1200, 800));
        setBackground(Color.WHITE);

        try { truckImg = ImageIO.read(new File("resources/truck.png")); }
        catch (IOException e) { truckImg = null; }

        setToolTipText("");
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e); }
        });

        /* trucks & timer */
        for (int i = 0; i < routes.size(); i++)
            truckStates.add(new TruckState(routes.get(i), "DA" + (i+1)));

        animationTimer = new Timer(30, e -> {
            truckStates.forEach(TruckState::tick);
            repaint();
        });
        animationTimer.start();
    }

    /* ---------- helpers ---------- */
    private int calcScale() {
        int max = Math.max(depot.x, depot.y);
        for (Route r : routes)
            for (Node n : r.customers)
                max = Math.max(max, Math.max(n.x, n.y));
        return Math.max(1, 500 / Math.max(max, 1));
    }

    private void updateHover(MouseEvent e) {
        int mx = e.getX() - (LEGEND_W + GAP_X);
        int my = e.getY() - GAP_Y;
        hoveredNode = null; hoverText = null;

        outer:
        for (Route r : routes) {
            for (int i = 0; i < r.customers.size(); i++) {
                Node n = r.customers.get(i);
                int nx = n.x * SCALE, ny = n.y * SCALE;
                if (Math.hypot(mx - nx, my - ny) <= NODE_RADIUS) {
                    hoveredNode = n;
                    int arr = r.arrival.get(i);
                    hoverText = String.format(
                            "C%d  Demand:%d  Arrival:%d  TW:%d-%d",
                            n.ID, n.demand, arr, n.ready, n.due);
                    break outer;
                }
            }
        }
        setToolTipText(hoverText);
        repaint();
    }

    /* ---------- painting ---------- */
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(LEGEND_W + GAP_X, GAP_Y);

        /* draw static routes & customers (unchanged look) */
        for (int idx = 0; idx < routes.size(); idx++) {
            Route r = routes.get(idx);
            g2.setColor(COLORS[idx % COLORS.length]);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Node prev = depot;
            for (Node n : r.customers) {
                int x1 = prev.x*SCALE, y1 = prev.y*SCALE;
                int x2 = n.x*SCALE,     y2 = n.y*SCALE;
                int cx = (x1+x2)/2 + (idx - routes.size()/2)*5;
                int cy = (y1+y2)/2 + 20;
                g2.draw(new QuadCurve2D.Float(x1,y1,cx,cy,x2,y2));
                prev = n;
            }
            g2.draw(new QuadCurve2D.Float(
                    prev.x*SCALE, prev.y*SCALE,
                    (prev.x+depot.x)*SCALE/2, (prev.y+depot.y)*SCALE/2+20,
                    depot.x*SCALE, depot.y*SCALE));

            /* customers – original white halos */
            for (Node n : r.customers) {
                int cx = n.x*SCALE, cy = n.y*SCALE, rad = NODE_RADIUS;
                for (int layer = 0; layer < 5 && rad > 0; layer++, rad -= 2) {
                    float alpha = 0.25f + 0.1f*layer;
                    g2.setColor(new Color(1f,1f,1f,alpha));
                    g2.fillOval(cx - rad, cy - rad, rad*2, rad*2);
                    g2.setColor(new Color(0f,0f,0f,alpha));
                    g2.drawOval(cx - rad, cy - rad, rad*2, rad*2);
                }
            }
        }

        /* animated trucks */
        for (TruckState ts : truckStates) drawTruck(g2, ts);

        /* depot */
        g2.setColor(Color.BLACK);
        int dx = depot.x*SCALE - NODE_RADIUS;
        int dy = depot.y*SCALE - NODE_RADIUS;
        g2.fillRect(dx, dy, NODE_RADIUS*2, NODE_RADIUS*2);
        g2.drawString("Depot", dx - 4, dy - 4);

        /* hover ring */
        if (hoveredNode != null) {
            g2.setColor(Color.YELLOW);
            int hx = hoveredNode.x*SCALE, hy = hoveredNode.y*SCALE;
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(hx - NODE_RADIUS - 4, hy - NODE_RADIUS - 4,
                        (NODE_RADIUS + 4)*2, (NODE_RADIUS + 4)*2);
        }
    }

    private void drawTruck(Graphics2D g2, TruckState ts) {
        if (ts.progress >= 1.0) return;

        double scaled = ts.progress * (ts.path.size()-1);
        int from = (int) Math.floor(scaled);
        int to   = Math.min(from + 1, ts.path.size() - 1);
        double t = scaled - from;

        Node a = ts.path.get(from), b = ts.path.get(to);
        int x = (int)((1 - t)*a.x + t*b.x)*SCALE;
        int y = (int)((1 - t)*a.y + t*b.y)*SCALE;

        if (truckImg != null) {
            g2.drawImage(truckImg, x - 12, y - 28, 24, 24, null);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(Color.WHITE); g2.drawString(ts.label, x + 14, y - 5);
            g2.setColor(Color.BLACK); g2.drawString(ts.label, x + 13, y - 6);
        }
    }

    /* ---------- inner class for animation ---------- */
    private class TruckState {
        final java.util.List<Node> path = new ArrayList<>();
        final String label;
        double progress = 0.0;
        final double speed;

        TruckState(Route route, String label) {
            this.label = label;
            path.add(depot); path.addAll(route.customers); path.add(depot);

            double dist = Math.max(1.0, route.calculateTotalDistance(depot));
            this.speed = 8.0 / (dist * 20);  // 20 = speed factor
        }
        void tick() { progress = Math.min(progress + speed, 1.0); }
    }

    /* ---------- launcher ---------- */
    public static void showGUI(Node depot, ArrayList<Route> routes,
                               String summary, String algorithmName) {
        RouteVisualizer map = new RouteVisualizer(depot, routes, summary);

        /* legend panel */
        JPanel legend = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(240,240,240));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("Colour ↔ Route", 20, 30);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                int y = 52;
                for (int i = 0; i < routes.size(); i++) {
                    g2.setColor(COLORS[i % COLORS.length]);
                    g2.fillRect(20, y - 11, 16, 16);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(20, y - 11, 16, 16);
                    g2.drawString("R" + (i+1) + " (DA" + (i+1) + ")", 45, y + 2);
                    y += 22;
                }
            }
            @Override public Dimension getPreferredSize() { return new Dimension(LEGEND_W, 800); }
        };

        /* top bar with pause button */
        JToggleButton pauseBtn = new JToggleButton("⏸ Pause");
        pauseBtn.addActionListener(e -> {
            if (pauseBtn.isSelected()) { map.animationTimer.stop(); pauseBtn.setText("▶ Resume"); }
            else                       { map.animationTimer.start(); pauseBtn.setText("⏸ Pause"); }
        });
        JLabel algLabel = new JLabel("Algorithm: " + algorithmName);
        algLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(pauseBtn);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(algLabel);

        /* summary text */
        JTextArea ta = new JTextArea(summary);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setLineWrap(true); ta.setWrapStyleWord(true);

        /* layout */
        JPanel horiz = new JPanel(new BorderLayout());
        horiz.add(legend, BorderLayout.WEST);
        horiz.add(map,    BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                          new JScrollPane(horiz),
                                          new JScrollPane(ta));
        split.setResizeWeight(0.85);
        split.setDividerLocation(650);

        JFrame f = new JFrame("Route Visualiser");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(topBar, BorderLayout.NORTH);
        f.add(split,  BorderLayout.CENTER);
        f.setSize(1300, 880);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
