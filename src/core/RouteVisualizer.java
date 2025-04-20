// ============================
// File: RouteVisualizer.java
// ============================
package core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class RouteVisualizer extends JPanel {
    private Node depot;
    private ArrayList<Route> routes;
    private int SCALE;
    private static final int RADIUS = 5;
    private static final Color[] COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.PINK, Color.CYAN, Color.YELLOW, Color.GRAY, Color.LIGHT_GRAY
    };

    public RouteVisualizer(Node depot, ArrayList<Route> routes) {
        this.depot = depot;
        this.routes = routes;
        this.SCALE = calculateScale(routes, depot);
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.WHITE);
        setToolTipText("");
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
    }

    private static int calculateScale(ArrayList<Route> routes, Node depot) {
        int maxX = depot.x, maxY = depot.y;
        for (Route route : routes) {
            for (Node n : route.customers) {
                maxX = Math.max(maxX, n.x);
                maxY = Math.max(maxY, n.y);
            }
        }
        int max = Math.max(maxX, maxY);
        return Math.max(1, 500 / Math.max(max, 1));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (Route route : routes) {
            for (Node node : route.customers) {
                int px = node.x * SCALE;
                int py = node.y * SCALE;
                if (e.getX() >= px && e.getX() <= px + RADIUS * 2 &&
                    e.getY() >= py && e.getY() <= py + RADIUS * 2) {
                    return "C" + node.ID + " (Demand: " + node.demand + ")";
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw depot
        g.setColor(Color.BLACK);
        g.fillOval(depot.x * SCALE, depot.y * SCALE, RADIUS * 2, RADIUS * 2);
        g.drawString("Depot", depot.x * SCALE, depot.y * SCALE);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2)); // Thicker lines

        // Draw each route with a different color
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            g2d.setColor(COLORS[i % COLORS.length]);

            Node prev = depot;
            for (Node customer : route.customers) {
                int x1 = prev.x * SCALE + RADIUS;
                int y1 = prev.y * SCALE + RADIUS;
                int x2 = customer.x * SCALE + RADIUS;
                int y2 = customer.y * SCALE + RADIUS;
                g2d.drawLine(x1, y1, x2, y2);

                g2d.fillOval(customer.x * SCALE, customer.y * SCALE, RADIUS * 2, RADIUS * 2);
                g2d.drawString("C" + customer.ID, customer.x * SCALE, customer.y * SCALE);
                prev = customer;
            }

            // Draw return to depot
            g2d.drawLine(prev.x * SCALE + RADIUS, prev.y * SCALE + RADIUS,
                    depot.x * SCALE + RADIUS, depot.y * SCALE + RADIUS);

            // Label route info
            int midX = (depot.x + prev.x) * SCALE / 2;
            int midY = (depot.y + prev.y) * SCALE / 2;
            double dist = route.calculateTotalDistance(depot);
            int eta = (int) Math.ceil(dist);
            String label = "Route " + (i + 1) +
                    " | Load: " + route.load +
                    " | Dist: " + String.format("%.1f", dist) +
                    " | ETA: " + eta + " min";
            g2d.drawString(label, midX, midY - 10);
        }
    }

    public static void showGUI(Node depot, ArrayList<Route> routes) {
        JFrame frame = new JFrame("Route Visualizer");
        RouteVisualizer panel = new RouteVisualizer(depot, routes);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> panel.exportAsImage("output/route_map.png"));
    }

    public void exportAsImage(String filename) {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        paint(g2);
        try {
            File output = new File(filename);
            output.getParentFile().mkdirs();
            ImageIO.write(image, "png", output);
            System.out.println("[✔] Route map exported to: " + output.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

