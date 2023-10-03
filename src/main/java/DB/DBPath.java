package main.java.DB;

import java.util.LinkedList;

/**
 * Represents a path through the DB railway net.
 */
public class DBPath {
    public DBPath(LinkedList<Long> nodes, double length) {
        this.nodes = nodes;
        this.length = length;
    }
    public LinkedList<Long> nodes;
    public double length;
}
