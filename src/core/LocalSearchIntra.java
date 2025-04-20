package core;

import java.util.ArrayList;

public class LocalSearchIntra {
    public void improveRoutes(ArrayList<Route> routes, Node depot) {
        boolean improvement = true;

        while (improvement) {
            improvement = false;

            for (Route route : routes) {
                double bestDelta = 0;
                int bestFrom = -1, bestTo = -1;

                for (int i = 0; i < route.customers.size(); i++) {
                    Node n = route.customers.remove(i);
                    for (int j = 0; j <= route.customers.size(); j++) {
                        route.customers.add(j, n);
                        double newDist = route.calculateTotalDistance(depot);
                        route.customers.remove(j);

                        double originalDist = route.calculateTotalDistance(depot);
                        double delta = originalDist - newDist;

                        if (delta > bestDelta) {
                            bestDelta = delta;
                            bestFrom = i;
                            bestTo = j;
                        }
                    }
                    route.customers.add(i, n);
                }

                if (bestDelta > 0 && bestFrom != -1 && bestTo != -1) {
                    Node n = route.customers.remove(bestFrom);
                    route.customers.add(bestTo, n);
                    improvement = true;
                }
            }
        }
    }
}

