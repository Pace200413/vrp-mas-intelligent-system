package core;

import java.util.ArrayList;

public class RouteLogger {
    public static void printSummary(ArrayList<Route> routes, Node depot) {
        double totalDist = 0;
        int totalLoad = 0;
        int routeNum = 1;

        System.out.println("\n======= ROUTE SUMMARY =======");
        for (Route route : routes) {
            double dist = route.calculateTotalDistance(depot);
            totalDist += dist;
            totalLoad += route.load;
            System.out.printf("Route %d: Distance = %.2f, Load = %d\n", routeNum++, dist, route.load);
        }

        System.out.println("------------------------------");
        System.out.printf("Total Routes: %d\n", routes.size());
        System.out.printf("Total Distance: %.2f\n", totalDist);
        System.out.printf("Total Load: %d\n", totalLoad);
        System.out.println("==============================\n");
    }
}

