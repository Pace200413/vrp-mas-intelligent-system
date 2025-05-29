package core;

import java.util.*;

/** Greedy solver that respects capacity *and* time windows (with 1-min tolerance). */
public class NearestNeighborSolver {

    private static final double EPS = 1.0;   // 1-minute slack

    public ArrayList<Route> generateRoutes(VRPInstance inst, int vehicleCapacity) {
        ArrayList<Route> routes = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        while (visited.size() < inst.customers.size()) {
            Route route = new Route();
            route.capacity = vehicleCapacity;

            Node prev = inst.depot;
            double time = 0;

            while (true) {
                Node next = null;
                double best = Double.MAX_VALUE;

                for (Node c : inst.customers) {
                    if (visited.contains(c.ID) || !route.canAdd(c)) continue;
                    if (!canServe(prev, c, time)) continue;

                    double d = prev.distanceTo(c);
                    if (d < best) { best = d; next = c; }
                }
                if (next == null) break;   // no feasible customer left

                /* update clock */
                double travel = prev.distanceTo(next);
                double arrive = time + travel;
                double wait   = Math.max(0, next.ready - arrive);
                time = arrive + wait + next.service;

                route.addCustomer(next);
                visited.add(next.ID);
                prev = next;
            }
            route.updateArrivals(inst.depot);
            routes.add(route);
        }
        return routes;
    }

    /** Check if arrival would violate due-time */
    private boolean canServe(Node prev, Node cand, double currentTime) {
        double travel = prev.distanceTo(cand);
        double arrive = currentTime + travel;
        return arrive <= cand.due + EPS;
    }
}
