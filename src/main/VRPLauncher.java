package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VRPLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VRPLauncher::launchGui);
    }

    private static void launchGui() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame frame = new JFrame("VRP Launcher – Full System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JComboBox<String> algorithmBox = new JComboBox<>(
        	    new String[]{
        	      "Nearest Neighbor",
        	      "Local Search (Intra)",
        	      "Local Search (Inter)",
        	      "Genetic Algorithm",
        	      "Simulated Annealing"
        	    });
        JSpinner agentSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
        JSpinner customerSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 300, 1));
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 500, 1));
        JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(1234, 1, Integer.MAX_VALUE, 1));

        panel.add(new JLabel("Optimisation Algorithm:")); panel.add(algorithmBox);
        panel.add(new JLabel("Delivery Agents:"));        panel.add(agentSpinner);
        panel.add(new JLabel("Customers:"));             panel.add(customerSpinner);
        panel.add(new JLabel("Vehicle Capacity:"));      panel.add(capacitySpinner);
        panel.add(new JLabel("Random Seed:"));           panel.add(seedSpinner);

        JButton runButton = new JButton("Run with JADE Agents");
        panel.add(new JLabel()); panel.add(runButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        runButton.addActionListener(e -> {
            String algorithm = (String) algorithmBox.getSelectedItem();
            int numAgents    = (Integer) agentSpinner.getValue();
            int numCustomers = (Integer) customerSpinner.getValue();
            int vehicleCap   = (Integer) capacitySpinner.getValue();
            int seed         = (Integer) seedSpinner.getValue();

            try {
                Runtime rt = Runtime.instance();
                Profile p = new ProfileImpl();
                p.setParameter(Profile.GUI, "true");
                ContainerController cc = rt.createMainContainer(p);

                Object[] mraArgs = { algorithm, numAgents, numCustomers, vehicleCap, seed };
                AgentController mra = cc.createNewAgent("mra", "agents.MasterRoutingAgent", mraArgs);
                mra.start();

                for (int i = 0; i < numAgents; i++) {
                	Object[] daArgs = { vehicleCap };
                	AgentController da = cc.createNewAgent("da" + i, "agents.DeliveryAgent", daArgs);
                    da.start();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to start JADE agents: " + ex.getMessage());
            }
        });
    }
}

