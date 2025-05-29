package core;

import java.util.*;

public class SimulatedAnnealingSolver {
    private final VRPInstance inst;
    private final int capacity;
    private final Random rnd;
    private double startTemp;
    private double endTemp;
    private int steps;
    
    // Adaptive SA 
    private static final int BASE_RESTARTS = 100;
    private static final int LARGE_INSTANCE_THRESHOLD = 100;
    private static final int VERY_LARGE_INSTANCE_THRESHOLD = 200;
    private static final int LARGE_INSTANCE_RESTARTS = 200;
    private static final int VERY_LARGE_INSTANCE_RESTARTS = 300;
    
    // SA parameters
    private static final double INITIAL_ACCEPT_RATE = 0.8; // Target initial acceptance rate
    private static final double MIN_ACCEPT_RATE = 0.1;    // Minimum acceptance rate
    private static final double COOLING_RATE = 0.95;      // Base cooling rate
    private static final double MIN_TEMP = 0.1;           // Minimum temperature
    private static final double MAX_TEMP = 1000.0;        // Maximum temperature
    private static final double TEMP_INCREASE = 1.5;      // Temperature increase factor on restart
    private static final int MAX_NO_IMPROVE = 50;         // Max iterations without improvement
    
    // Neighborhood structures
    private static final int NEIGHBORHOOD_2OPT = 0;
    private static final int NEIGHBORHOOD_OROPT = 1;
    private static final int NEIGHBORHOOD_EXCHANGE = 2;
    private static final int NEIGHBORHOOD_RELOCATE = 3;
    private static final int NEIGHBORHOOD_CROSS = 4;
    private static final int NUM_NEIGHBORHOODS = 5;

    public SimulatedAnnealingSolver(VRPInstance inst, int capacity, 
                                  double startTemp, double endTemp, int steps) {
        this.inst = inst;
        this.capacity = capacity;
        this.startTemp = startTemp;
        this.endTemp = endTemp;
        this.steps = steps;
        this.rnd = new Random();
        
        // Adjust parameters based on instance size
        if (inst.customers.size() > VERY_LARGE_INSTANCE_THRESHOLD) {
            this.steps = (int)(this.steps * 1.5);
        } else if (inst.customers.size() > LARGE_INSTANCE_THRESHOLD) {
            this.steps = (int)(this.steps * 1.2);
        }
    }
    
    // Get number of restarts based on instance size
    private int getNumRestarts() {
        int numCustomers = inst.customers.size();
        if (numCustomers > VERY_LARGE_INSTANCE_THRESHOLD) {
            return VERY_LARGE_INSTANCE_RESTARTS;
        } else if (numCustomers > LARGE_INSTANCE_THRESHOLD) {
            return LARGE_INSTANCE_RESTARTS;
        }
        return BASE_RESTARTS;
    }
    
    public SimulatedAnnealingSolver(VRPInstance inst, int capacity) {
        this(inst, capacity, 1000, 1, 10_000);
    }

    // Generate a neighbor solution 
    private ArrayList<Route> generateNeighbor(ArrayList<Route> solution) {
        ArrayList<Route> neighbor = deepCopy(solution);
        
        // Select neighborhood structure based on instance size
        int neighborhoodType = rnd.nextInt(NUM_NEIGHBORHOODS);
        boolean success = false;
        
        // Try the selected neighborhood structure
        switch (neighborhoodType) {
            case NEIGHBORHOOD_2OPT:
                success = apply2Opt(neighbor);
                break;
            case NEIGHBORHOOD_OROPT:
                success = applyOrOpt(neighbor);
                break;
            case NEIGHBORHOOD_EXCHANGE:
                success = applyExchange(neighbor);
                break;
            case NEIGHBORHOOD_RELOCATE:
                success = applyRelocate(neighbor);
                break;
            case NEIGHBORHOOD_CROSS:
                success = applyCrossExchange(neighbor);
                break;
        }
        
        // If the selected neighborhood didn't produce a valid move, try others
        if (!success) {
            for (int i = 0; i < NUM_NEIGHBORHOODS && !success; i++) {
                if (i == neighborhoodType) continue;
                
                switch (i) {
                    case NEIGHBORHOOD_2OPT:
                        success = apply2Opt(neighbor);
                        break;
                    case NEIGHBORHOOD_OROPT:
                        success = applyOrOpt(neighbor);
                        break;
                    case NEIGHBORHOOD_EXCHANGE:
                        success = applyExchange(neighbor);
                        break;
                    case NEIGHBORHOOD_RELOCATE:
                        success = applyRelocate(neighbor);
                        break;
                    case NEIGHBORHOOD_CROSS:
                        success = applyCrossExchange(neighbor);
                        break;
                }
            }
        }
        
        return neighbor;
    }
    
    // Apply 2-opt intra-route optimization
    private boolean apply2Opt(ArrayList<Route> solution) {
        if (solution.isEmpty()) return false;
        
        Route route = solution.get(rnd.nextInt(solution.size()));
        if (route.customers.size() < 4) return false;
        
        int i = 1 + rnd.nextInt(route.customers.size() - 2);
        int j = i + 1 + rnd.nextInt(route.customers.size() - i - 1);
        
        // Reverse the sub-route between i and j
        Collections.reverse(route.customers.subList(i, j + 1));
        return true;
    }
    
    // Apply Or-opt move (relocate a sequence of 1-3 consecutive nodes)
    private boolean applyOrOpt(ArrayList<Route> solution) {
        if (solution.size() < 2) return false;
        
        // Select a random route and sequence
        int r1 = rnd.nextInt(solution.size());
        if (solution.get(r1).customers.size() < 2) return false;
        
        int start = rnd.nextInt(solution.get(r1).customers.size());
        int length = Math.min(1 + rnd.nextInt(3), solution.get(r1).customers.size() - start);
        
        // Select target route (could be the same route)
        int r2 = rnd.nextInt(solution.size());
        if (r1 == r2 && solution.get(r1).customers.size() <= length) return false;
        
        // Extract the sequence
        List<Node> sequence = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            sequence.add(solution.get(r1).customers.remove(start));
        }
        
        // Calculate total demand of the sequence
        int seqDemand = sequence.stream().mapToInt(n -> n.demand).sum();
        
        // Check capacity if moving to a different route
        if (r1 != r2 && solution.get(r2).load + seqDemand > capacity) {
            // Move failed, restore the route
            solution.get(r1).customers.addAll(start, sequence);
            return false;
        }
        
        // Find insertion point in target route
        int insertPos = r2 == r1 && start < solution.get(r2).customers.size() ? 
            rnd.nextInt(solution.get(r2).customers.size() + 1) : 
            rnd.nextInt(solution.get(r2).customers.size() + 1);
        
        // Update loads
        solution.get(r1).load -= seqDemand;
        solution.get(r2).load += seqDemand;
        
        // Insert sequence
        solution.get(r2).customers.addAll(insertPos, sequence);
        
        // Remove empty routes (except the last one)
        if (solution.get(r1).customers.isEmpty() && solution.size() > 1) {
            solution.remove(r1);
        }
        
        return true;
    }
    
    // Exchange two customers between routes
    private boolean applyExchange(ArrayList<Route> solution) {
        if (solution.size() < 2) return false;
        
        int r1 = rnd.nextInt(solution.size());
        int r2 = rnd.nextInt(solution.size());
        if (r1 == r2 || solution.get(r1).customers.isEmpty() || solution.get(r2).customers.isEmpty()) {
            return false;
        }
        
        int i1 = rnd.nextInt(solution.get(r1).customers.size());
        int i2 = rnd.nextInt(solution.get(r2).customers.size());
        
        Node c1 = solution.get(r1).customers.get(i1);
        Node c2 = solution.get(r2).customers.get(i2);
        
        // Check capacity constraints
        if (solution.get(r1).load - c1.demand + c2.demand <= capacity &&
            solution.get(r2).load - c2.demand + c1.demand <= capacity) {
            
            // Perform the swap
            solution.get(r1).customers.set(i1, c2);
            solution.get(r2).customers.set(i2, c1);
            
            // Update loads
            solution.get(r1).load += (c2.demand - c1.demand);
            solution.get(r2).load += (c1.demand - c2.demand);
            
            return true;
        }
        return false;
    }
    
    // Relocate a customer to a different position
    private boolean applyRelocate(ArrayList<Route> solution) {
        if (solution.size() < 2) return false;
        
        // Find a non-empty route to move from
        Route fromRoute = null;
        int fromIdx = -1;
        for (int i = 0; i < 10; i++) { // Try up to 10 times to find a suitable route
            int r = rnd.nextInt(solution.size());
            if (!solution.get(r).customers.isEmpty()) {
                fromRoute = solution.get(r);
                fromIdx = r;
                break;
            }
        }
        if (fromRoute == null) return false;
        
        // Choose a customer to move
        int custIdx = rnd.nextInt(fromRoute.customers.size());
        Node customer = fromRoute.customers.get(custIdx);
        
        // Find a different route to move to
        int toIdx;
        do {
            toIdx = rnd.nextInt(solution.size());
        } while (toIdx == fromIdx && solution.size() > 1);
        
        Route toRoute = solution.get(toIdx);
        
        // Check capacity constraint
        if (toRoute.load + customer.demand > capacity) {
            return false;
        }
        
        // Remove from original route
        fromRoute.customers.remove(custIdx);
        fromRoute.load -= customer.demand;
        
        // Add to new route at random position
        if (toRoute.customers.isEmpty()) {
            toRoute.customers.add(customer);
        } else {
            int insertPos = rnd.nextInt(toRoute.customers.size() + 1);
            toRoute.customers.add(insertPos, customer);
        }
        toRoute.load += customer.demand;
        
        // Remove empty routes (except the last one)
        if (fromRoute.customers.isEmpty() && solution.size() > 1) {
            solution.remove(fromIdx);
        }
        
        return true;
    }
    
    // Cross-exchange between two routes
    private boolean applyCrossExchange(ArrayList<Route> solution) {
        if (solution.size() < 2) return false;
        
        int r1 = rnd.nextInt(solution.size());
        int r2 = rnd.nextInt(solution.size() - 1);
        if (r2 >= r1) r2++;
        
        Route route1 = solution.get(r1);
        Route route2 = solution.get(r2);
        
        if (route1.customers.size() < 2 || route2.customers.size() < 2) {
            return false;
        }
        
        // Select segments to swap (at least 1 customer, leaving at least 1 customer)
        int start1 = rnd.nextInt(route1.customers.size());
        int end1 = start1 + 1 + rnd.nextInt(route1.customers.size() - start1);
        
        int start2 = rnd.nextInt(route2.customers.size());
        int end2 = start2 + 1 + rnd.nextInt(route2.customers.size() - start2);
        
        // Extract segments
        List<Node> seg1 = new ArrayList<>(route1.customers.subList(start1, end1));
        List<Node> seg2 = new ArrayList<>(route2.customers.subList(start2, end2));
        
        // Calculate new loads
        int load1 = route1.load - seg1.stream().mapToInt(n -> n.demand).sum() 
                                 + seg2.stream().mapToInt(n -> n.demand).sum();
        int load2 = route2.load - seg2.stream().mapToInt(n -> n.demand).sum()
                                 + seg1.stream().mapToInt(n -> n.demand).sum();
        
        // Check capacity constraints
        if (load1 > capacity || load2 > capacity) {
            return false;
        }
        
        // Perform the exchange
        route1.customers.subList(start1, end1).clear();
        route1.customers.addAll(start1, seg2);
        route1.load = load1;
        
        route2.customers.subList(start2, end2).clear();
        route2.customers.addAll(start2, seg1);
        route2.load = load2;
        
        return true;
    }
    
    public ArrayList<Route> solve() {
        ArrayList<Route> overallBest = null;
        double overallBestFit = Double.POSITIVE_INFINITY;
        
        int numRestarts = getNumRestarts();
        System.out.println("Using " + numRestarts + " restarts for " + inst.customers.size() + " customers");
        
        // Track restart statistics
        int[] improvements = new int[numRestarts];
        double[] bestFits = new double[numRestarts];
        
        for (int restart = 0; restart < numRestarts; restart++) {
            // Initialize with a mix of random and NN solutions
            ArrayList<Route> current;
            if (restart % 3 == 0) {
                // Every 3rd restart, use a completely random solution
                current = generateRandomSolution();
            } else {
                // Otherwise use NN with some randomness
                NearestNeighborSolver nn = new NearestNeighborSolver();
                current = nn.generateRoutes(inst, capacity);
                // Apply some random perturbations using the new neighborhood structures
                for (int i = 0; i < 5; i++) {
                    current = generateNeighbor(current);
                }
            }
            
            double currFit = fitness(current);
            ArrayList<Route> best = deepCopy(current);
            double bestFit = currFit;
            
            // Adaptive temperature based on initial solution quality
            double temperature = Math.min(MAX_TEMP, 
                Math.max(MIN_TEMP, currFit * 0.1));
            
            int noImproveIter = 0;
            int accepted = 0;
            int totalMoves = 0;

            // Main SA loop
            while (noImproveIter < MAX_NO_IMPROVE) {
                // Generate multiple neighbors using the improved neighborhood structures
                int neighborsToTry = (int)(1 + (temperature / MAX_TEMP) * 4);
                ArrayList<Route> bestCandidate = null;
                double bestCandidateFit = Double.POSITIVE_INFINITY;
                
                // Evaluate multiple neighbors, pick the best
                for (int n = 0; n < neighborsToTry; n++) {
                    ArrayList<Route> candidate = generateNeighbor(current);
                    double candFit = fitness(candidate);
                    
                    if (candFit < bestCandidateFit) {
                        bestCandidate = candidate;
                        bestCandidateFit = candFit;
                    }
                    totalMoves++;
                }
                
                // Calculate acceptance probability with adaptive temperature
                double delta = bestCandidateFit - currFit;
                double acceptProb = Math.exp(-delta / temperature);
                
                // Accept if better or with probability based on temperature
                if (delta < 0 || rnd.nextDouble() < acceptProb) {
                    current = bestCandidate;
                    currFit = bestCandidateFit;
                    accepted++;
                    
                    // Update best solution if improved
                    if (currFit < bestFit) {
                        best = deepCopy(current);
                        bestFit = currFit;
                        noImproveIter = 0;
                        improvements[restart]++;
                    }
                } else {
                    noImproveIter++;
                }
                
                // Adaptive cooling based on acceptance rate
                double acceptRate = (double)accepted / totalMoves;
                if (acceptRate > INITIAL_ACCEPT_RATE) {
                    temperature *= 0.9; // Cool faster if accepting too many
                } else if (acceptRate < MIN_ACCEPT_RATE) {
                    temperature *= 1.1; // Heat up if accepting too few
                } else {
                    temperature *= COOLING_RATE; // Normal cooling
                }
                
                temperature = Math.max(MIN_TEMP, Math.min(MAX_TEMP, temperature));
                
                // Early restart if temperature gets too low
                if (temperature <= MIN_TEMP * 1.1) {
                    break;
                }
            }

            // Track best solution across all restarts
            if (bestFit < overallBestFit) {
                overallBest = deepCopy(best);
                overallBestFit = bestFit;
            }
            bestFits[restart] = bestFit;

            // Print progress
            if ((restart + 1) % 10 == 0) {
                System.out.printf("Restart %3d/%d - Best: %.2f (Current: %.2f, Temp: %.2f)\n",
                    restart + 1, numRestarts, overallBestFit, bestFit, temperature);
            }

            // Increase temperature for next restart to encourage exploration
            temperature = Math.min(MAX_TEMP, temperature * TEMP_INCREASE);
        }

        // Print summary
        System.out.println("\n--- SA Restart Summary ---");
        System.out.printf("Best solution found: %.2f\n", overallBestFit);
        System.out.printf("Average improvement per restart: %.2f\n", 
            Arrays.stream(improvements).average().orElse(0));

        return overallBest;
    }

    private ArrayList<Route> generateRandomSolution() {
        // Create a random solution by shuffling customers into random routes
        ArrayList<Node> customers = new ArrayList<>(inst.customers);
        Collections.shuffle(customers, rnd);

        ArrayList<Route> solution = new ArrayList<>();
        Route currentRoute = new Route();
        currentRoute.capacity = capacity;

        for (Node customer : customers) {
            if (currentRoute.load + customer.demand > capacity) {
                solution.add(currentRoute);
                currentRoute = new Route();
                currentRoute.capacity = capacity;
            }
            currentRoute.customers.add(customer);
            currentRoute.load += customer.demand;
        }
        if (!currentRoute.customers.isEmpty()) {
            solution.add(currentRoute);
        }
        return solution;
    }

    // Helper method to insert a customer using cheapest insertion heuristic
    private void insertCustomerCheapest(ArrayList<Route> solution, Node customer) {
        double bestCost = Double.POSITIVE_INFINITY;
        int bestRouteIdx = -1;
        int bestPos = -1;

        // Try inserting into existing routes
        for (int r = 0; r < solution.size(); r++) {
            Route route = solution.get(r);
            if (route.load + customer.demand > capacity) continue;

            for (int i = 0; i <= route.customers.size(); i++) {
                double cost = calculateInsertionCost(route, customer, i);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestRouteIdx = r;
                    bestPos = i;
                }
            }
        }

        // If no possible insertion found, create new route
        if (bestRouteIdx == -1) {
            Route newRoute = new Route();
            newRoute.capacity = capacity;
            newRoute.customers.add(customer);
            newRoute.load = customer.demand;
            solution.add(newRoute);
        } else {
            // Insert into best position
            Route route = solution.get(bestRouteIdx);
            route.customers.add(bestPos, customer);
            route.load += customer.demand;
        }
    }

    // Calculate the cost of inserting a customer at a specific position
    private double calculateInsertionCost(Route route, Node customer, int position) {
        if (position == 0) {
            return inst.depot.distanceTo(customer) + 
                   (route.customers.isEmpty() ? 0 : customer.distanceTo(route.customers.get(0)));
        } else if (position == route.customers.size()) {
            return route.customers.get(position-1).distanceTo(customer) + 
                   customer.distanceTo(inst.depot);
        } else {
            Node prev = route.customers.get(position-1);
            Node next = route.customers.get(position);
            return prev.distanceTo(customer) + customer.distanceTo(next) - prev.distanceTo(next);
        }
    }

    // Fitness function for SA 
    private double fitness(ArrayList<Route> sol) {
        double dist = 0;
        double twPenalty = 0;
        double loadPenalty = 0;
        double routePenalty = sol.size() * 1000; // Penalty for number of routes

        // Distance and time window penalties
        for (Route r : sol) {
            dist += r.calculateTotalDistance(inst.depot);
            Node prev = inst.depot;
            double t = 0;

            for (Node n : r.customers) {
                t += prev.distanceTo(n);
                t = Math.max(t, n.ready);
                if (t > n.due) twPenalty += (t - n.due);  // Penalize lateness
                t += n.service;
                prev = n;

                // Add a small penalty for load imbalance
                loadPenalty += Math.pow(r.load / (double)r.capacity, 2);
            }
        }
        
        // Weighted sum of objectives
        return dist + 100 * twPenalty + 10 * loadPenalty + routePenalty;
    }

    private ArrayList<Route> deepCopy(ArrayList<Route> sol) {
        ArrayList<Route> copy = new ArrayList<>();
        for (Route r : sol) {
            Route nr = new Route();
            nr.capacity = r.capacity;
            nr.load = r.load;
            nr.customers = new ArrayList<>(r.customers);
            copy.add(nr);
        }
        return copy;
    }
}
