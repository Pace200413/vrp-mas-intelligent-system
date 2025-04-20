package core;

import java.util.ArrayList;
import java.util.Random;

public class VRPInstance {
    public Node depot;
    public ArrayList<Node> customers = new ArrayList<>();
    public double[][] distanceMatrix;

    public void generateInstance(int seed) {
        depot = new Node(0, 50, 50, 0);
        Random ran = new Random(seed);

        for (int i = 1; i <= 30; i++) {
            int x = ran.nextInt(100);
            int y = ran.nextInt(100);
            int demand = 4 + ran.nextInt(7);
            customers.add(new Node(i, x, y, demand));
        }

        computeDistanceMatrix();
    }

    private void computeDistanceMatrix() {
        int size = customers.size() + 1;
        distanceMatrix = new double[size][size];
        ArrayList<Node> all = new ArrayList<>();
        all.add(depot);
        all.addAll(customers);

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                distanceMatrix[i][j] = all.get(i).distanceTo(all.get(j));
    }
}

