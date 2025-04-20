package core;

import java.io.*;
import java.util.ArrayList;

public class CSVLoader {
    public static ArrayList<Node> loadCustomersFromCSV(String filePath) {
        ArrayList<Node> customers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; } // Skip CSV header
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0].trim());
                    int x = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    int demand = Integer.parseInt(parts[3].trim());
                    customers.add(new Node(id, x, y, demand));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return customers;
    }
}

