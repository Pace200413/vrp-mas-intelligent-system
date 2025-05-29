package core;

import java.util.*;

public class GeneticAlgorithmSolver {
    private VRPInstance inst;
    private int capacity;
    private int populationSize, generations;
    private double mutationRate;
    private Map<Integer, Double> fitnessCache = new HashMap<>();
    private Random rnd = new Random();

    // Now take capacity as a parameter
    public GeneticAlgorithmSolver(VRPInstance inst, int capacity,
                                  int popSize, int gens, double mutRate) {
        this.inst = inst;
        this.capacity = capacity;
        this.populationSize = popSize;
        this.generations   = gens;
        this.mutationRate  = mutRate;
    }

    public ArrayList<Route> solve() {
        // Run the GA multiple times and choose the best solution
        final int NUM_RUNS = 3;
        List<Route> overallBestSolution = null;
        double overallBestFitness = Double.MAX_VALUE;
        
        System.out.println("\nRunning Genetic Algorithm " + NUM_RUNS + " times...");
        
        for (int run = 0; run < NUM_RUNS; run++) {
            System.out.println("GA Run " + (run + 1) + "/" + NUM_RUNS);
            
            // Clear cache between runs
            fitnessCache.clear();
            
            // Standard GA procedure for each run
            List<List<Route>> pop = initPopulation();
            double bestFitness = Double.MAX_VALUE;
            int noImprovementCount = 0;
            List<Route> bestSolution = null;
            
            for (int gen = 0; gen < generations && noImprovementCount < 20; gen++) {
                // Sort population by fitness
                pop.sort(Comparator.comparingDouble(this::fitness));
                
                // Check for improvement
                double currentBest = fitness(pop.get(0));
                if (currentBest < bestFitness) {
                    bestFitness = currentBest;
                    bestSolution = new ArrayList<>(pop.get(0));
                    noImprovementCount = 0;
                    
                    // Apply local search to best solution
                    if (gen % 5 == 0) { // Apply local search every 5 generations
                        localSearch(bestSolution);
                    }
                } else {
                    noImprovementCount++;
                }
                
                // Create next generation
                List<List<Route>> next = new ArrayList<>();
                
                // Elitism - keep the best solutions
                int eliteCount = Math.max(2, populationSize / 10);
                for (int i = 0; i < eliteCount && i < pop.size(); i++) {
                    next.add(new ArrayList<>(pop.get(i)));
                }
                
                // Update mutation rate adaptively
                double adaptiveMutationRate = getAdaptiveMutationRate(pop);
                
                // Create offspring
                while (next.size() < populationSize) {
                    List<Route> p1 = tournamentSelect(pop);
                    List<Route> p2 = tournamentSelect(pop);
                    List<Route> child = crossover(p1, p2);
                    mutate(child, adaptiveMutationRate);
                    
                    // Accept based on simulated annealing occasionally
                    if (rnd.nextDouble() < 0.1) { // 10% chance to apply simulated annealing
                        double temperature = 1.0 - ((double) gen / generations); // Decreases with generations
                        double childFitness = fitness(child);
                        double parentFitness = fitness(p1);
                        double delta = childFitness - parentFitness;
                        
                        if (delta < 0 || Math.exp(-delta / temperature) > rnd.nextDouble()) {
                            next.add(child);
                        } else {
                            // If rejected, add a clone of the parent
                            next.add(new ArrayList<>(p1));
                        }
                    } else {
                        next.add(child);
                    }
                }
                
                pop = next;
                
                // Clear cache periodically to prevent memory issues
                if (gen % 10 == 0) {
                    fitnessCache.clear();
                }
            }
            
            // Process this run's result
            List<Route> finalSolution;
            if (bestSolution != null) {
                finalSolution = bestSolution;
            } else {
                pop.sort(Comparator.comparingDouble(this::fitness));
                finalSolution = pop.get(0);
            }
            
            // Ensure all customers are included in the solution
            finalSolution = ensureAllCustomersInSolution(finalSolution);
            
            // Calculate final fitness
            double finalFitness = calculateTotalDistance(finalSolution);
            
            System.out.println("Run " + (run + 1) + " completed. Solution quality: " + finalFitness);
            
            // Check if this is the best solution across all runs
            if (finalFitness < overallBestFitness) {
                overallBestFitness = finalFitness;
                overallBestSolution = new ArrayList<>(finalSolution);
                System.out.println("→ New best solution found!");
            }
        }
        
        System.out.println("\nBest solution found with quality: " + overallBestFitness);
        return new ArrayList<>(overallBestSolution);
    }

    private List<List<Route>> initPopulation() {
        List<List<Route>> pop = new ArrayList<>();
        
        // Use different initialization strategies for diversity
        for (int i = 0; i < populationSize; i++) {
            ArrayList<Route> base;
            
            // Choose strategy based on position in population
            if (i < populationSize * 0.6) {
                // 60% nearest neighbor
                NearestNeighborSolver nn = new NearestNeighborSolver();
                base = nn.generateRoutes(inst, capacity);
                
                // Apply some random swaps to create diversity
                if (rnd.nextDouble() < 0.5) {
                    for (Route r : base) {
                        if (r.customers.size() >= 2) {
                            for (int j = 0; j < r.customers.size() / 3; j++) {
                                int a = rnd.nextInt(r.customers.size());
                                int b = rnd.nextInt(r.customers.size());
                                Collections.swap(r.customers, a, b);
                            }
                        }
                    }
                }
            } else {
                // 40% random solutions with greedy insertion
                base = generateGreedySolution();
            }
            
            pop.add(new ArrayList<>(base));
        }
        return pop;
    }

    private double fitness(List<Route> sol) {
        // Check cache first to avoid recalculation
        Integer solHash = calculateRouteListHashCode(sol);
        if (fitnessCache.containsKey(solHash)) {
            return fitnessCache.get(solHash);
        }
        
        double dist = 0;
        double penalty = 0;

        for (Route r : sol) {
            dist += r.calculateTotalDistance(inst.depot);
            r.updateArrivals(inst.depot);
            for (int i = 0; i < r.customers.size(); i++) {
                Node n = r.customers.get(i);
                int arrival = r.arrival.get(i);
                if (arrival > n.due) {
                    // Weighted penalty based on how late the arrival is
                    penalty += 1000 + 10 * (arrival - n.due);  
                } else if (arrival < n.ready - 2) {
                    // Weighted penalty based on how early the arrival is
                    penalty += 1000 + 10 * (n.ready - arrival);  
                }
            }
        }
        
        double result = dist + penalty;
        fitnessCache.put(solHash, result);
        return result;
    }


    private List<Route> tournamentSelect(List<List<Route>> pop) {
        List<Route> best = null;
        double bestFit = Double.MAX_VALUE;
        
        // Increase tournament size for better selection pressure
        int tournamentSize = Math.max(3, populationSize / 10);
        for (int i = 0; i < tournamentSize; i++) {
            List<Route> cand = pop.get(rnd.nextInt(pop.size()));
            double f = fitness(cand);
            if (f < bestFit) { 
                bestFit = f; 
                best = cand; 
            }
        }
        return best;
    }

    private List<Route> crossover(List<Route> p1, List<Route> p2) {
        // Choose crossover strategy randomly
        int strategy = rnd.nextInt(2);
        
        if (strategy == 0) {
            return orderCrossover(p1, p2);
        } else {
            return routeBasedCrossover(p1, p2);
        }
    }
    
    private List<Route> orderCrossover(List<Route> p1, List<Route> p2) {
        // Order Crossover (OX)
        int start = rnd.nextInt(p1.size());
        int end = start;
        if (p1.size() > 1) {
            end = start + 1 + rnd.nextInt(p1.size() - 1);
            if (end >= p1.size()) end = p1.size() - 1;
        }
        
        List<Route> child = new ArrayList<>();
        // Copy a segment from p1
        for (int i = start; i <= end && i < p1.size(); i++) {
            Route original = p1.get(i);
            Route copy = new Route();
            copy.capacity = original.capacity;
            // Deep copy all customers
            for (Node n : original.customers) {
                copy.addCustomer(n);
            }
            child.add(copy);
        }
        
        // Track used customer IDs
        Set<Integer> used = new HashSet<>();
        for (Route r: child) {
            for (Node n: r.customers) {
                used.add(n.ID);
            }
        }
        
        // Add remaining routes from p2, respecting capacity
        for (Route r : p2) {
            Route copy = new Route();
            copy.capacity = r.capacity;
            for (Node n : r.customers) {
                if (!used.contains(n.ID) && copy.canAdd(n)) {
                    copy.addCustomer(n);
                    used.add(n.ID);
                }
            }
            if (!copy.customers.isEmpty()) child.add(copy);
        }
        
        // Ensure all customers are included
        return ensureAllCustomersInSolution(child);
    }
    
    private List<Route> routeBasedCrossover(List<Route> p1, List<Route> p2) {
        // Traditional route-based crossover
        int cut = p1.size()/2;
        if (p1.size() <= 1) cut = 0;
        
        List<Route> child = new ArrayList<>();
        // Copy first half of routes from p1
        for (int i = 0; i < cut; i++) {
            Route original = p1.get(i);
            Route copy = new Route();
            copy.capacity = original.capacity;
            // Deep copy all customers
            for (Node n : original.customers) {
                copy.addCustomer(n);
            }
            child.add(copy);
        }
        
        // Track used customer IDs
        Set<Integer> used = new HashSet<>();
        for (Route r: child) {
            for (Node n: r.customers) {
                used.add(n.ID);
            }
        }
        
        // Add routes from p2 with unused customers
        for (Route r : p2) {
            Route copy = new Route();
            copy.capacity = r.capacity;
            for (Node n : r.customers) {
                if (!used.contains(n.ID) && copy.canAdd(n)) {
                    copy.addCustomer(n);
                    used.add(n.ID);
                }
            }
            if (!copy.customers.isEmpty()) child.add(copy);
        }
        
        // Ensure all customers are included
        return ensureAllCustomersInSolution(child);
    }

    private void mutate(List<Route> sol, double adaptiveMutationRate) {
        for (Route r : sol) {
            if (r.customers.size() >= 2 && rnd.nextDouble() < adaptiveMutationRate) {
                // Choose mutation strategy
                int strategy = rnd.nextInt(4);
                
                switch (strategy) {
                    case 0: // Swap mutation
                        int a = rnd.nextInt(r.customers.size());
                        int b = rnd.nextInt(r.customers.size());
                        Collections.swap(r.customers, a, b);
                        break;
                        
                    case 1: // Inversion mutation
                        int start = rnd.nextInt(r.customers.size());
                        int end = rnd.nextInt(r.customers.size());
                        if (start > end) {
                            int temp = start;
                            start = end;
                            end = temp;
                        }
                        // Reverse the segment
                        if (start != end) {
                            Collections.reverse(r.customers.subList(start, end + 1));
                        }
                        break;
                        
                    case 2: // Insert mutation
                        if (r.customers.size() > 2) {
                            int pos1 = rnd.nextInt(r.customers.size());
                            int pos2 = rnd.nextInt(r.customers.size());
                            if (pos1 != pos2) {
                                Node node = r.customers.remove(pos1);
                                r.customers.add(pos2, node);
                            }
                        }
                        break;
                        
                    case 3: // Scramble mutation
                        if (r.customers.size() > 3) {
                            int scrambleStart = rnd.nextInt(r.customers.size() - 2);
                            int scrambleLength = 2 + rnd.nextInt(r.customers.size() - scrambleStart - 1);
                            int scrambleEnd = Math.min(scrambleStart + scrambleLength, r.customers.size());
                            List<Node> subList = r.customers.subList(scrambleStart, scrambleEnd);
                            Collections.shuffle(subList);
                        }
                        break;
                }
            }
        }
        
        // Inter-route mutation: occasionally move a customer from one route to another
        if (sol.size() >= 2 && rnd.nextDouble() < adaptiveMutationRate * 0.5) {
            int routeFrom = rnd.nextInt(sol.size());
            int routeTo = rnd.nextInt(sol.size());
            
            // Make sure source route has customers and we're not moving to the same route
            if (routeFrom != routeTo && !sol.get(routeFrom).customers.isEmpty()) {
                Route fromRoute = sol.get(routeFrom);
                Route toRoute = sol.get(routeTo);
                
                // Pick a random customer from source route
                int customerIdx = rnd.nextInt(fromRoute.customers.size());
                Node customer = fromRoute.customers.get(customerIdx);
                
                // Try to add to destination route
                if (toRoute.canAdd(customer)) {
                    fromRoute.customers.remove(customerIdx);
                    toRoute.addCustomer(customer);
                }
            }
        }
    }
    
    // Calculate adaptive mutation rate based on population diversity
    private double getAdaptiveMutationRate(List<List<Route>> population) {
        // Simple diversity measure based on fitness variance
        double avgFitness = population.stream()
                .mapToDouble(this::fitness)
                .average()
                .orElse(0.0);
        
        double variance = population.stream()
                .mapToDouble(sol -> Math.pow(fitness(sol) - avgFitness, 2))
                .average()
                .orElse(0.0);
                
        double diversity = Math.sqrt(variance); // Standard deviation
        
        // Normalize diversity to produce a mutation rate between 0.01 and 0.25
        // When diversity is high, use lower mutation rate
        // When diversity is low, use higher mutation rate to explore more
        double normalizedDiversity = Math.min(1.0, diversity / avgFitness);
        return Math.max(0.01, Math.min(0.25, mutationRate * (1.0 - 0.5 * normalizedDiversity)));
    }
    
    // Local search optimization with 2-opt
    private void localSearch(List<Route> solution) {
        boolean improved = true;
        int iterations = 0;
        
        while (improved && iterations < 10) { // Limit iterations to prevent endless loops
            improved = false;
            double currentFitness = fitness(solution);
            
            // Try 2-opt on each route
            for (Route r : solution) {
                if (r.customers.size() < 3) continue;
                
                for (int i = 0; i < r.customers.size() - 1 && !improved; i++) {
                    for (int j = i + 1; j < r.customers.size() && !improved; j++) {
                        // Swap nodes i and j
                        Collections.swap(r.customers, i, j);
                        
                        // Check if it improves
                        double newFitness = fitness(solution);
                        if (newFitness < currentFitness) {
                            currentFitness = newFitness;
                            improved = true;
                        } else {
                            // Revert the swap
                            Collections.swap(r.customers, i, j);
                        }
                    }
                }
            }
            
            iterations++;
        }
    }
    
    // Generate a greedy solution by inserting customers one by one
    private ArrayList<Route> generateGreedySolution() {
        ArrayList<Route> routes = new ArrayList<>();
        List<Node> unassigned = new ArrayList<>(inst.customers);
        Collections.shuffle(unassigned);
        
        while (!unassigned.isEmpty()) {
            Route r = new Route();
            r.capacity = capacity;
            
            if (unassigned.isEmpty()) break;
            
            // Start with a random node
            Node firstNode = unassigned.remove(rnd.nextInt(unassigned.size()));
            r.addCustomer(firstNode);
            
            // Add remaining nodes using greedy insertion
            boolean added;
            do {
                added = false;
                Node bestNode = null;
                int bestPosition = -1;
                double bestIncrease = Double.MAX_VALUE;
                
                for (Node n : unassigned) {
                    if (!r.canAdd(n)) continue;
                    
                    // Try inserting at each position
                    for (int pos = 0; pos <= r.customers.size(); pos++) {
                        // Insert temporarily
                        r.customers.add(pos, n);
                        double newDist = r.calculateTotalDistance(inst.depot);
                        r.customers.remove(pos); // Remove for now
                        
                        // Calculate the insertion cost
                        double increase = newDist - r.calculateTotalDistance(inst.depot);
                        
                        if (increase < bestIncrease) {
                            bestIncrease = increase;
                            bestNode = n;
                            bestPosition = pos;
                        }
                    }
                }
                
                // Insert the best node if found
                if (bestNode != null) {
                    r.customers.add(bestPosition, bestNode);
                    unassigned.remove(bestNode);
                    added = true;
                }
            } while (added && !unassigned.isEmpty());
            
            routes.add(r);
        }
        
        return routes;
    }
    
    // Helper method to calculate consistent hash code for route lists
    private Integer calculateRouteListHashCode(List<Route> routes) {
        int hash = 0;
        for (Route r : routes) {
            int routeHash = 0;
            for (Node n : r.customers) {
                routeHash = 31 * routeHash + n.ID;
            }
            hash = 31 * hash + routeHash;
        }
        return hash;
    }
    
    // Ensure all customers are included in the solution
    private List<Route> ensureAllCustomersInSolution(List<Route> solution) {
        // Identify which customers are in the solution
        Set<Integer> includedIds = new HashSet<>();
        for (Route r : solution) {
            for (Node n : r.customers) {
                includedIds.add(n.ID);
            }
        }
        
        // Check if we have all customers
        if (includedIds.size() == inst.customers.size()) {
            return solution; // All customers are included
        }
        
        // Find missing customers
        List<Node> missingCustomers = new ArrayList<>();
        for (Node n : inst.customers) {
            if (!includedIds.contains(n.ID)) {
                missingCustomers.add(n);
            }
        }
        
        System.out.println("WARNING: Found " + missingCustomers.size() + " missing customers in solution");
        
        // Add missing customers
        for (Node n : missingCustomers) {
            // Try to add to existing routes first
            boolean added = false;
            
            // First try: add to route with lowest load that can accommodate
            List<Route> sortedRoutes = new ArrayList<>(solution);
            sortedRoutes.sort(Comparator.comparingInt(r -> r.load));
            
            for (Route r : sortedRoutes) {
                if (r.canAdd(n)) {
                    r.addCustomer(n);
                    added = true;
                    break;
                }
            }
            
            // If couldn't add to existing routes, create a new route
            if (!added) {
                Route newRoute = new Route();
                newRoute.capacity = capacity;
                newRoute.addCustomer(n);
                solution.add(newRoute);
            }
        }
        
        return solution;
    }
    
    // Helper method to calculate total distance for a solution
    private double calculateTotalDistance(List<Route> solution) {
        double totalDistance = 0;
        for (Route r : solution) {
            totalDistance += r.calculateTotalDistance(inst.depot);
        }
        return totalDistance;
    }
}
