package core;

import java.util.*;

public class NearestNeighborSolver {
    public ArrayList<Route> generateRoutes(VRPInstance instance) {
        ArrayList<Route> routes = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        while (visited.size() < instance.customers.size()) {
            Route route = new Route();
            Node current = instance.depot;

            while (true) {
                Node next = findNearestFeasible(current, instance.customers, visited, route);
                if (next == null) break;
                route.addCustomer(next);
                visited.add(next.ID);
                current = next;
            }

            routes.add(route);
        }

        return routes;
    }

    private Node findNearestFeasible(Node current, List<Node> customers, Set<Integer> visited, Route route) {
        Node nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Node c : customers) {
            if (visited.contains(c.ID)) continue;
            if (!route.canAdd(c)) continue;

            double dist = current.distanceTo(c);
            if (dist < minDist) {
                minDist = dist;
                nearest = c;
            }
        }

        return nearest;
    }

    public void printRoutes(ArrayList<Route> routes, Node depot) {
        int i = 1;
        for (Route route : routes) {
            System.out.println("Route " + i++ + ": " + route);
            System.out.printf("  Load: %d / %d | Distance: %.2f\n", route.load, route.capacity, route.calculateTotalDistance(depot));
        }
    }
}

