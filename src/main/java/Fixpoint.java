package main.java;

import main.java.DB.DBNode;
import main.java.OSM.OSMNode;

public class Fixpoint {
    public Fixpoint(OSMNode osmNode, DBNode dbNode) {
        this.osmNode = osmNode;
        this.dbNode = dbNode;
    }
    public OSMNode osmNode;
    public DBNode dbNode;
}
