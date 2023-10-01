package main.java.OSM;

import java.util.ArrayList;

public class OSMPath {
    public OSMPath(ArrayList<Long> nodes, double length) {
        this.nodes = nodes;
        this.length = length;
    }

    public ArrayList<Long> nodes;
    public double length;
}
