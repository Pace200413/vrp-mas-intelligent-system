package agents;

import core.*;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import javax.swing.SwingUtilities;
import java.util.*;

/**
 * Master Routing Agent (MRA).
 *  • Waits for capacity announcements from all Delivery Agents.
 *  • Generates VRPTW instance, solves, and sends routes.
 *  • Handles re-assignments and fallback routes if a DA refuses.
 */
public class MasterRoutingAgent extends Agent {

    private VRPInstance vrp;
    private ArrayList<Route> routes;

    private final Map<String, Integer> agentCaps = new HashMap<>();
    private int expectedCaps;

    private String algorithm;
    private int numAgents, numCustomers, seed;

    @Override
    protected void setup() {
        Object[] a = getArguments();
        algorithm    = (String)  a[0];
        numAgents    = (Integer) a[1];
        numCustomers = (Integer) a[2];
        int vehicleCap = (Integer) a[3];
        seed         = (Integer) a[4];

        expectedCaps = numAgents;

        System.out.printf(
            "[INFO] %s waiting for %d capacity messages (alg=%s, customers=%d, seed=%d)%n",
            getLocalName(), expectedCaps, algorithm, numCustomers, seed);

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                switch (msg.getConversationId()) {
                case "capacity-announcement":
                    handleCapacity(msg);
                    break;
                case "vrp-route-refused":
                    handleRefusal(msg);
                    break;
                }
            }
        });
    }

    private void handleCapacity(ACLMessage msg) {
        agentCaps.put(msg.getSender().getLocalName(), Integer.parseInt(msg.getContent()));
        System.out.printf("[MRA] Capacity from %s → %s%n",
                          msg.getSender().getLocalName(), msg.getContent());

        if (agentCaps.size() == expectedCaps) {
            launchOptimisation();
        }
    }

    private void launchOptimisation() {
        vrp = new VRPInstance();
        vrp.generateInstance(seed, numCustomers);

        int maxCap = agentCaps.values().stream().mapToInt(i -> i).max().orElse(50);
        routes = solveWith(algorithm, vrp, maxCap);
        routes.forEach(r -> r.updateArrivals(vrp.depot));

        sendRoutesToAgents();

        String summary = RouteLogger.buildSummary(routes, vrp.depot);
        System.out.println(summary);

//        System.out.println("[DEBUG] enqueue GUI, alg=" + algorithm + "  routes=" + routes.size());

        SwingUtilities.invokeLater(() -> {
//            System.out.println("[DEBUG] inside EDT, building frame");
            RouteVisualizer.showGUI(vrp.depot, routes, summary, algorithm);
        });
    }

    private void sendRoutesToAgents() {
        List<Map.Entry<String,Integer>> agents = new ArrayList<>(agentCaps.entrySet());
        agents.sort((a, b) -> b.getValue() - a.getValue());

        int cursor = 0;
        for (Route r : routes) {
            boolean sent = false;
            int start    = cursor;

            do {
                String agName = agents.get(cursor).getKey();
                int    cap    = agents.get(cursor).getValue();

                if (r.load <= cap) {
                    dispatchRouteTo(agName, r);
                    sent   = true;
                    cursor = (cursor + 1) % agents.size();
                    break;
                }
                cursor = (cursor + 1) % agents.size();
            } while (cursor != start);

            if (!sent) {
                int minCap = agents.get(agents.size() - 1).getValue();
                List<Route> chunks = r.splitByCapacity(minCap);
                routes.addAll(chunks);
            }
        }
    }

    private void handleRefusal(ACLMessage msg) {
        System.err.printf("[MRA] %s refused route → re-queuing%n", msg.getSender().getLocalName());

        int refusedLoad = 0;
        try {
            String loadPart = msg.getContent().split("Load")[1];
            refusedLoad = Integer.parseInt(loadPart.replaceAll("[^0-9].*", ""));
        } catch (Exception ignored) {}

        int minCap = agentCaps.values().stream().mapToInt(Integer::intValue).min().orElse(30);
        Route dummy = new Route();
        dummy.capacity = minCap;
        dummy.load = refusedLoad;

        routes.addAll(dummy.splitByCapacity(minCap));
        sendRoutesToAgents();
    }

    private boolean tryReassign(Route rejected, String failedAgent) {
        for (Map.Entry<String, Integer> entry : agentCaps.entrySet()) {
            String agentName = entry.getKey();
            int    cap       = entry.getValue();

            if (agentName.equals(failedAgent)) continue;
            if (rejected.load > cap) continue;

            Node depot = vrp.depot;
            boolean twOk = true;
            for (Node n : rejected.customers) {
                twOk &= rejected.canAddTW(depot, n);
            }

            if (!twOk) continue;

            dispatchRouteTo(agentName, rejected);
            return true;
        }
        return false;
    }

    private void dispatchRouteTo(String agentName, Route r) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        msg.setConversationId("vrp-route");

        double dist = r.calculateTotalDistance(vrp.depot);
        int eta     = (int) Math.ceil(dist);

        msg.setContent(String.format(
                "Route: %s | Load: %d/%d | Dist: %.2f | ETA: %d min",
                r, r.load, r.capacity, dist, eta));
        send(msg);
        System.out.printf("[MRA] Sent route to %s (load %d/%d)%n",
                agentName, r.load, r.capacity);
    }

    private ArrayList<Route> solveWith(String alg, VRPInstance inst, int cap) {
        ArrayList<Route> out;
        long start = System.nanoTime();

        switch (alg) {
            case "Local Search (Intra)":
                out = new NearestNeighborSolver().generateRoutes(inst, cap);
                new LocalSearchIntra().improveRoutes(out, inst.depot);
                break;
            case "Local Search (Inter)":
                out = new NearestNeighborSolver().generateRoutes(inst, cap);
                new LocalSearchInter().improveRoutes(out, inst.depot);
                break;
            case "Genetic Algorithm":
                out = new GeneticAlgorithmSolver(inst, cap, 50, 200, 0.05).solve();
                break;
            case "Simulated Annealing":
                out = new SimulatedAnnealingSolver(inst, cap, 1000, 1, 10_000).solve();
                break;
            default:
                out = new NearestNeighborSolver().generateRoutes(inst, cap);
                break;
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("[INFO] %s computed in %d ms%n", alg, durationMs);
        return out;
    }
}
