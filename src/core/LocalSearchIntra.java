package core;

import java.util.ArrayList;

public class LocalSearchIntra {
    public void improveRoutes(ArrayList<Route> routes, Node depot) {
        boolean improvement = true;

        while (improvement) {
            improvement = false;

            for (Route route : routes) {
                double bestDelta = 0;
                int bestFrom = -1;
                int bestTo = -1;

                for (int i = 0; i < route.customers.size(); i++) {
                    for (int j = 0; j < route.customers.size(); j++) {
                        if (i == j) continue;

                        Node n = route.customers.get(i);
                        route.customers.remove(i);
                        route.customers.add(j, n);

                        route.updateArrivals(depot);
                        boolean twOk = true;
                        for (int k = 0; k < route.customers.size(); k++) {
                            Node node = route.customers.get(k);
                            int arrival = route.arrival.get(k);
                            if (arrival > node.due || arrival < node.ready - 2) {
                                twOk = false;
                                break;
                            }
                        }

                        double dist = route.calculateTotalDistance(depot);

                        // Undo move
                        route.customers.remove(j);
                        route.customers.add(i, n);

                        if (twOk) {
                            double origDist = route.calculateTotalDistance(depot);
                            double delta = origDist - dist;

                            if (delta > bestDelta) {
                                bestDelta = delta;
                                bestFrom = i;
                                bestTo = j;
                            }
                        }
                    }
                }

                if (bestFrom != -1 && bestTo != -1) {
                    Node n = route.customers.remove(bestFrom);
                    route.customers.add(bestTo, n);
                    improvement = true;
                }
            }
        }

        // Final update of arrival times to support visualisation
        for (Route route : routes) {
            route.updateArrivals(depot);
        }
    }
}
