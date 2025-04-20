// ==========================
// File: MasterRoutingAgent.java
// ==========================
package agents;

import core.*;
import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class MasterRoutingAgent extends Agent {
    protected void setup() {
        System.out.println("[INFO] " + getLocalName() + " started.");

        // 1. Generate VRP Instance
        VRPInstance vrp = new VRPInstance();
        vrp.generateInstance(151292); // Use your birth seed
        System.out.println("[INFO] Generating VRP instance with 30 customers...");

        // 2. Nearest Neighbor Heuristic
        NearestNeighborSolver solver = new NearestNeighborSolver();
        ArrayList<Route> routes = solver.generateRoutes(vrp);
        System.out.println("[INFO] Nearest Neighbor heuristic applied.");

        // 3. Intra-route Local Search
        LocalSearchIntra intra = new LocalSearchIntra();
        System.out.println("[INFO] Applying Intra-route local search...");
        intra.improveRoutes(routes, vrp.depot);

        // 4. Inter-route Local Search
        LocalSearchInter inter = new LocalSearchInter();
        System.out.println("[INFO] Applying Inter-route local search...");
        inter.improveRoutes(routes, vrp.depot);

        // 5. Print Final Routes
        System.out.println("[INFO] Final optimized routes:\n");
        solver.printRoutes(routes, vrp.depot);

        // 6. Sniffer Test Message (visible in Sniffer)
        ACLMessage testMsg = new ACLMessage(ACLMessage.INFORM);
        testMsg.addReceiver(new AID("da0", AID.ISLOCALNAME));
        testMsg.setContent("Hello from mra");
        testMsg.setConversationId("test-message");
        testMsg.setLanguage("English");
        send(testMsg);

        // 7. Send Routes to Delivery Agents
        System.out.println("[INFO] Distributing routes to Delivery Agents...\n");
        int agentId = 0;
        for (Route route : routes) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("da" + agentId, AID.ISLOCALNAME));
            msg.setConversationId("vrp-route");
            msg.setLanguage("English");

            StringBuilder sb = new StringBuilder();
            double dist = route.calculateTotalDistance(vrp.depot);
            
            sb.append("Route: Depot -> ");
            for (Node c : route.customers) {
                sb.append("C").append(c.ID).append(" -> ");
            }
            sb.append("Depot | ");
            sb.append("Load: ").append(route.load).append(" / ").append(route.capacity).append(" | ");
            sb.append("Distance: ").append(String.format("%.2f", dist));
            int eta = (int) Math.ceil(dist); // simple ETA: 1 unit distance = 1 minute
            sb.append(" | ETA: ").append(eta).append(" min");
            msg.setContent(sb.toString());
            send(msg);

            System.out.println("[MRA] Sent route to da" + agentId + ": " + msg.getContent());

            agentId++;
            if (agentId >= 5) break; 
            
            // Only send to 5 agents max
        }
        RouteVisualizer.showGUI(vrp.depot, routes);
    }
}

