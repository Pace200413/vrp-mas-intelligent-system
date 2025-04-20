package agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class DeliveryAgent extends Agent {

    protected void setup() {
        System.out.println("[INFO] " + getLocalName() + " started and waiting for route...");

        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("[" + getLocalName() + "] received: " + msg.getContent());
                } else {
                    block(); // Wait for message
                }
            }
        });
    }
}

