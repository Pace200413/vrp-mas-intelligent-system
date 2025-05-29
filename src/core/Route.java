package core;

import java.util.ArrayList;

/** A single vehicle route. */
public class Route {

    public ArrayList<Node> customers = new ArrayList<>();
    public ArrayList<Integer> arrival = new ArrayList<>();   // parallel list

    public int capacity = 50;
    public int load     = 0;

    /* ---------- capacity helpers ---------- */
    public boolean canAdd(Node customer) {
        return load + customer.demand <= capacity;
    }
    public void addCustomer(Node customer) {
        customers.add(customer);
        load += customer.demand;
    }

    /* ---------- time-window helpers ---------- */
    /** Quick feasibility check: would appending c violate any TW? */
    public boolean canAddTW(Node depot, Node c) {
        double t = 0; Node prev = depot;
        for (Node n : customers) {
            t += prev.distanceTo(n);          // travel
            t  = Math.max(t, n.ready);        // wait if early
            t += n.service;                  // service
            if (t > n.due) return false;
            prev = n;
        }
        /* append candidate */
        t += prev.distanceTo(c);
        t  = Math.max(t, c.ready);
        t += c.service;
        return t <= c.due;
    }

    /** Recomputes arrival[] for visualisation / verification */
    public void updateArrivals(Node depot) {
        arrival.clear();
        double t = 0; Node prev = depot;
        for (Node n : customers) {
            t += prev.distanceTo(n);
            t  = Math.max(t, n.ready);
            arrival.add((int) t);
            t += n.service;
            prev = n;
        }
    }

    /* ---------- distance ---------- */
    public double calculateTotalDistance(Node depot) {
        double d = 0; Node prev = depot;
        for (Node n : customers) { d += prev.distanceTo(n); prev = n; }
        return d + prev.distanceTo(depot);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Depot -> ");
        for (Node n : customers) sb.append("C").append(n.ID).append(" -> ");
        return sb.append("Depot").toString();
    }
    public java.util.List<Route> splitByCapacity(int cap) {

        java.util.List<Route> chunks = new java.util.ArrayList<>();
        Route current = new Route();
        current.capacity = cap;

        for (Node n : customers) {
            /* If adding this customer exceeds the cap, start a new chunk */
            if (current.load + n.demand > cap && !current.customers.isEmpty()) {
                chunks.add(current);
                current = new Route();
                current.capacity = cap;
            }
            current.addCustomer(n);
        }
        /* add the final chunk (if any customers were placed in it) */
        if (!current.customers.isEmpty()) chunks.add(current);

        return chunks;
    }
}
