package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class MainContainer {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        ContainerController container = rt.createMainContainer(p);

        try {
            AgentController mra = container.createNewAgent("mra", "agents.MasterRoutingAgent", null);
            mra.start();

            for (int i = 0; i < 5; i++) {
                AgentController da = container.createNewAgent("da" + i, "agents.DeliveryAgent", null);
                da.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
