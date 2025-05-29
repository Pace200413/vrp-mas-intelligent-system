package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Delivery Agent (DA).
 *  • Chooses its own capacity unless a value is passed as an argument.
 *  • Announces that capacity to the Master Routing Agent (MRA).
 *  • Accepts or refuses the route it receives based on load.
 */
public class DeliveryAgent extends Agent {

    private int capacity = 30 + (int) (Math.random() * 51); // default 30–80

    @Override
    protected void setup() {

        /* --- override capacity if passed from the launcher --- */
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            capacity = Integer.parseInt(args[0].toString());
        }

        /* --- announce capacity to MRA --- */
        ACLMessage capMsg = new ACLMessage(ACLMessage.INFORM);
        capMsg.addReceiver(new AID("mra", AID.ISLOCALNAME));
        capMsg.setConversationId("capacity-announcement");
        capMsg.setContent(Integer.toString(capacity));
        send(capMsg);

        System.out.printf("[DA] %s ready. Capacity=%d%n", getLocalName(), capacity);

        /* --- main behaviour: wait for route assignments --- */
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                if ("vrp-route".equals(msg.getConversationId())) {
                    handleRoute(msg);
                }
            }
        });
    }

    /** Parse route message, accept or refuse based on load */
    private void handleRoute(ACLMessage msg) {
        String content = msg.getContent();
        int assignedLoad = parseLoad(content);

        if (assignedLoad > capacity) {
            System.err.printf("[DA] %s REJECTED route! Load %d > cap %d%n",
                              getLocalName(), assignedLoad, capacity);
            ACLMessage refuse = msg.createReply();
            refuse.setPerformative(ACLMessage.REFUSE);
            refuse.setConversationId("vrp-route-refused");
            refuse.setContent("Load " + assignedLoad + " > cap " + capacity);
            send(refuse);
        } else {
            System.out.printf("[DA] %s ACCEPTED route. Load: %d / %d%n",
                              getLocalName(), assignedLoad, capacity);
        }
    }

    /** Extract numeric load from “Load: 18/50 | …” */
    private int parseLoad(String content) {
        try {
            String loadPart = content.split("Load:")[1].split("\\|")[0].trim();
            return Integer.parseInt(loadPart.split("/")[0].trim());
        } catch (Exception e) {
            System.err.println("[DA] Failed to parse load: " + e.getMessage());
            return Integer.MAX_VALUE;
        }
    }
}
