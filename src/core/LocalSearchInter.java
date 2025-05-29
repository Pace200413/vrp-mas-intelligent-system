package core;

import java.util.ArrayList;

public class LocalSearchInter {
    public void improveRoutes(ArrayList<Route> routes, Node depot) {
        boolean improvement = true;

        while (improvement) {
            improvement = false;

            for (int i = 0; i < routes.size(); i++) {
                Route from = routes.get(i);

                for (int j = 0; j < from.customers.size(); j++) {
                    Node customer = from.customers.get(j);

                    for (int k = 0; k < routes.size(); k++) {
                        if (i == k) continue;

                        Route to = routes.get(k);
                        if (!to.canAdd(customer)) continue;

                        for (int l = 0; l <= to.customers.size(); l++) {
                            // Simulate move
                            from.customers.remove(j);
                            from.load -= customer.demand;

                            to.customers.add(l, customer);
                            to.load += customer.demand;

                            boolean twOk = true;
                            for (Node c : to.customers) {
                                if (!to.canAddTW(depot, c)) {
                                    twOk = false;
                                    break;
                                }
                            }

                            double before = from.calculateTotalDistance(depot) + to.calculateTotalDistance(depot);
                            double after = before;

                            if (twOk) {
                                after = from.calculateTotalDistance(depot) + to.calculateTotalDistance(depot);
                            }

                            if (twOk && after < before) {
                                improvement = true;
                                break;
                            } else {
                                // Rollback
                                to.customers.remove(l);
                                to.load -= customer.demand;
                                from.customers.add(j, customer);
                                from.load += customer.demand;
                            }
                        }
                        if (improvement) break;
                    }
                    if (improvement) break;
                }
                if (improvement) break;
            }
        }
    }
}
