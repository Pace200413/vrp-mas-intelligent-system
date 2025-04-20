// ============================
// File: RouteVisualizer.java
// ============================
package core;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class RouteVisualizer extends JPanel {
    private Node depot;
    private ArrayList<Route> routes;
    private static final int SCALE = 5;
    private static final int RADIUS = 5;
    private static final Color[] COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.PINK, Color.CYAN, Color.YELLOW, Color.GRAY, Color.LIGHT_GRAY
    };

    public RouteVisualizer(Node depot, ArrayList<Route> routes) {
        this.depot = depot;
        this.routes = routes;
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw depot
        g.setColor(Color.BLACK);
        g.fillOval(depot.x * SCALE, depot.y * SCALE, RADIUS * 2, RADIUS * 2);
        g.drawString("Depot", depot.x * SCALE, depot.y * SCALE);

        // Draw each route with a different color
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            g.setColor(COLORS[i % COLORS.length]);

            Node prev = depot;
            for (Node customer : route.customers) {
                int x1 = prev.x * SCALE + RADIUS;
                int y1 = prev.y * SCALE + RADIUS;
                int x2 = customer.x * SCALE + RADIUS;
                int y2 = customer.y * SCALE + RADIUS;
                g.drawLine(x1, y1, x2, y2);

                g.fillOval(customer.x * SCALE, customer.y * SCALE, RADIUS * 2, RADIUS * 2);
                g.drawString("C" + customer.ID, customer.x * SCALE, customer.y * SCALE);

                prev = customer;
            }

            // Draw return to depot
            g.drawLine(prev.x * SCALE + RADIUS, prev.y * SCALE + RADIUS,
                       depot.x * SCALE + RADIUS, depot.y * SCALE + RADIUS);
        }
    }

    public static void showGUI(Node depot, ArrayList<Route> routes) {
        JFrame frame = new JFrame("Route Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new RouteVisualizer(depot, routes));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
