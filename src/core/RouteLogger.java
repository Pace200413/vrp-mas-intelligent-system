package core;

import java.util.ArrayList;

/**
 * Logs routes with a 1-minute tolerance and professional “IMPOSSIBLE” reasons.
 */
public final class RouteLogger {

    private static final double EPS = 1.0;   // 1-minute slack

    public static String buildSummary(ArrayList<Route> routes, Node depot) {
        StringBuilder sb = new StringBuilder("======= ROUTE SUMMARY =======\n");
        double totalDist = 0; int totalLoad = 0; boolean anyBad = false; int idx = 1;

        for (Route r : routes) {
            int headerStart = sb.length();
            sb.append(String.format("R%-2d  Load:%d  Dist:%.1f%n",
                    idx++, r.load, r.calculateTotalDistance(depot)));

            double t = 0; Node prev = depot; boolean bad = false;

            for (Node n : r.customers) {

                double travel     = prev.distanceTo(n);
                double arriveRaw  = t + travel;
                double wait       = Math.max(0, n.ready - arriveRaw);
                double start      = arriveRaw + wait;

                boolean late      = start - EPS > n.due;
                double  winLen    = n.due - n.ready;
                boolean waitLong  = wait - EPS > winLen;

                String reason = "";
                if (waitLong) {
                    reason = String.format(
                        " (wait %d > window %d — vehicle could wait ≤2 min extra if schedule allows)",
                        (int)wait, (int)winLen);
                } else if (late) {
                    reason = String.format(
                        " (start %d > due %d — vehicle could wait ≤2 min extra if schedule allows)",
                        (int)start, n.due);
                }

                sb.append(String.format(
                    "      C%-2d  Arrival:%4d  TW:%4d-%4d  Demand:%2d  Wait:%3d%s%s%n",
                    n.ID, (int)start, n.ready, n.due, n.demand, (int)wait,
                    (late || waitLong) ? "  IMPOSSIBLE" : "", reason));

                if (late || waitLong) bad = true;

                t = start + n.service;
                prev = n;
            }

            if (bad) {
                anyBad = true;
                sb.insert(headerStart, "(INFEASIBLE) ");
            }
            totalDist += r.calculateTotalDistance(depot);
            totalLoad += r.load;
        }

        sb.append("------------------------------\n")
          .append(String.format("Total routes: %d%n", routes.size()))
          .append(String.format("Total distance: %.1f%n", totalDist))
          .append(String.format("Total load: %d%n", totalLoad))
          .append("==============================\n");

        if (anyBad)
            sb.append("⚠  One or more routes are INFEASIBLE (time-window violations).\n");

        return sb.toString();
    }

    private RouteLogger() {}  // utility
}
