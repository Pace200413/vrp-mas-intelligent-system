package core;

import java.util.ArrayList;
import java.util.Random;

/** Generates a synthetic VRP / VRPTW instance. */
public class VRPInstance {

    public Node depot;
    public ArrayList<Node> customers = new ArrayList<>();
    public double[][] distanceMatrix;

    public void generateInstance(int seed, int customerCount) {
        depot = new Node(0, 50, 50, 0);            // depot at centre
        Random ran = new Random(seed);
        customers.clear();

        for (int i = 1; i <= customerCount; i++) {
            int x = ran.nextInt(100);
            int y = ran.nextInt(100);
            int demand = 4 + ran.nextInt(7);

            int ready = 200 + ran.nextInt(400);         // 200–600
            int window = 100 + ran.nextInt(200);        // 100–300 long
            int due = ready + window;
            int service = 5 + ran.nextInt(6);           // 5–10 min

            customers.add(new Node(i, x, y, demand, ready, due, service));
        }
        computeDistanceMatrix();
    }

    /* Pre-compute pairwise distances */
    private void computeDistanceMatrix() {
        int size = customers.size() + 1;
        distanceMatrix = new double[size][size];

        ArrayList<Node> all = new ArrayList<>();
        all.add(depot); all.addAll(customers);

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                distanceMatrix[i][j] = all.get(i).distanceTo(all.get(j));
    }
}
