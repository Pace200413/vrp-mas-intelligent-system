package core;

public class Node {
    public int ID, x, y, demand;

    public Node(int ID, int x, int y, int demand) {
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.demand = demand;
    }

    public double distanceTo(Node other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

