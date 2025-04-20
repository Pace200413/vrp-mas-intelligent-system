package core;

import java.util.ArrayList;

public class Route {
    public ArrayList<Node> customers = new ArrayList<>();
    public int capacity = 50;
    public int load = 0;

    public boolean canAdd(Node customer) {
        return load + customer.demand <= capacity;
    }

    public void addCustomer(Node customer) {
        customers.add(customer);
        load += customer.demand;
    }

    public double calculateTotalDistance(Node depot) {
        double dist = 0;
        Node prev = depot;
        for (Node node : customers) {
            dist += prev.distanceTo(node);
            prev = node;
        }
        dist += prev.distanceTo(depot);
        return dist;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Depot -> ");
        for (Node n : customers)
            sb.append("C").append(n.ID).append(" -> ");
        sb.append("Depot");
        return sb.toString();
    }
}

