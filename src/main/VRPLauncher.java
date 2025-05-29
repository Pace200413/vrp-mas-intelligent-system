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
                p.setParameter(Profile.GUI, "true");  // This automatically starts the RMA agent
                ContainerController cc = rt.createMainContainer(p);
                
                // Small delay to ensure RMA GUI initializes properly
                Thread.sleep(1000);
                
                // Start Sniffer agent with properly configured arguments
                // Show a dialog to let the user know what's happening
                JOptionPane.showMessageDialog(frame,
                    "We'll now start the JADE platform with the Sniffer agent.\n" +
                    "After clicking OK, wait for the JADE interfaces to load completely.\n" +
                    "A new dialog will appear when you can start the agents.",
                    "JADE Initialization", JOptionPane.INFORMATION_MESSAGE);
                    
                // Create the JADE platform and Sniffer first
                try {
                    // Create but don't start the agents yet
                    final Object[] mraArgs = { algorithm, numAgents, numCustomers, vehicleCap, seed };
                    final AgentController mra = cc.createNewAgent("mra", "agents.MasterRoutingAgent", mraArgs);
                    
                    final AgentController[] deliveryAgents = new AgentController[numAgents];
                    for (int i = 0; i < numAgents; i++) {
                        Object[] daArgs = { vehicleCap };
                        deliveryAgents[i] = cc.createNewAgent("da" + i, "agents.DeliveryAgent", daArgs);
                    }
                    
                    // Start the Sniffer agent
                    AgentController sniffer = cc.createNewAgent("sniffer", "jade.tools.sniffer.Sniffer", null);
                    sniffer.start();
                    System.out.println("Started Sniffer agent successfully");
                    
                    // Wait for JADE to initialize properly
                    Thread.sleep(3000);
                    
                    // Create a non-modal dialog with instructions and a button to start the agents
                    JDialog dialog = new JDialog(frame, "Sniffer Setup Instructions", false);
                    dialog.setSize(500, 400);
                    dialog.setLayout(new BorderLayout());
                    
                    // Add instructions panel
                    JPanel instructionsPanel = new JPanel(new BorderLayout());
                    instructionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                    
                    JTextArea instructions = new JTextArea();
                    instructions.setEditable(false);
                    instructions.setLineWrap(true);
                    instructions.setWrapStyleWord(true);
                    instructions.setText(
                        "====== SNIFFER AGENT SETUP ======\n\n" +
                        "1. In the RMA window (with title 'rma@...'), right-click on 'sniffer' agent\n" +
                        "2. Select 'Start Sniffer' from the menu\n" +
                        "3. Wait for the Sniffer window to fully load (titled 'sniffer@...')\n" +
                        "4. In the Sniffer window:\n" +
                        "   - Right-click in the agents list (left panel)\n" +
                        "   - Select 'Do sniff on this agent' and choose 'mra'\n");
                    
                    // Add agent list to instructions
                    instructions.append("   - Also select these agents:\n");
                    for (int i = 0; i < numAgents; i++) {
                        instructions.append("     * da" + i + "\n");
                    }
                    
                    instructions.append("\n5. Once you've selected all the agents to monitor, click 'Start Agents' below\n");
                    instructions.append("\nNOTE: Make sure the Sniffer window is fully loaded before trying to select agents!");
                    
                    // Scroll pane for instructions
                    JScrollPane scrollPane = new JScrollPane(instructions);
                    instructionsPanel.add(scrollPane, BorderLayout.CENTER);
                    
                    // Add button panel
                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    JButton startButton = new JButton("Start Agents");
                    startButton.addActionListener(startEvent -> {
                        dialog.dispose();
                        
                        try {
                            // Start the MRA and DeliveryAgents
                            System.out.println("\nStarting agents now!");
                            
                            mra.start();
                            System.out.println("Started MasterRoutingAgent (mra)");
                            
                            for (int i = 0; i < numAgents; i++) {
                                deliveryAgents[i].start();
                                System.out.println("Started DeliveryAgent (da" + i + ")");
                            }
                        } catch (Exception ex) {
                            System.err.println("Error starting agents: " + ex.getMessage());
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, 
                                "Error starting agents: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    
                    buttonPanel.add(startButton);
                    
                    // Add components to dialog
                    dialog.add(instructionsPanel, BorderLayout.CENTER);
                    dialog.add(buttonPanel, BorderLayout.SOUTH);
                    dialog.setLocationRelativeTo(frame);
                    
                    // Show the dialog (this will block until user clicks the button)
                    dialog.setVisible(true);
                    
                } catch (Exception ex) {
                    System.out.println("Error: " + ex.getMessage());
                    ex.printStackTrace();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to start JADE agents: " + ex.getMessage());
            }
        });
    }
    

}
