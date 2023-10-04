package main.java.DB;

import java.util.LinkedList;

/**
 * Represents a path through the DB railway net.
 */
public class DBPath {
    public DBPath(LinkedList<Long> nodes, double length, int countOfBranches) {
        this.nodes = nodes;
        this.length = length;
        this.countOfBranches = countOfBranches;
    }
    public LinkedList<Long> nodes;
    public double length;
    public int countOfBranches;
}
