package main.java.OSM;

import java.util.ArrayList;

/**
 * Represents a path through the OSM railway net.
 */
public class OSMPath {
    public OSMPath(ArrayList<Long> nodes, double length, int countOfBranches) {
        this.nodes = nodes;
        this.length = length;
        this.countOfBranches = countOfBranches;
    }

    public ArrayList<Long> nodes;
    public double length;
    public int countOfBranches;
}
