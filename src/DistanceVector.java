public class DistanceVector {
    private int distance;
    private String nextHop;

    public DistanceVector (int distance, String nextHop) {
        this.distance = distance;
        this.nextHop = nextHop;
    }

    public int getDistance() { return distance; }

    public String getNextHop() { return nextHop; }

    public void setDistance(int distance) { this.distance = distance; }

    public void setNextHop(String nextHop) { this.nextHop = nextHop; }
}
