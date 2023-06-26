package main.java.DB;

import java.util.Map;

public class DBNode {

    public DBNode(int streckenId, long myDBNodeId, long sectionId, long elementId, long km,String type, String ds100, String stationName, String name1, String name2) {
        this.streckenId = streckenId;
        this.sectionId = sectionId;
        this.myDBNodeId = myDBNodeId;
        this. elementId = elementId;
        this.km = km;
        this.type = type;
        this.ds100 = ds100;
        this.stationName = stationName;
        this.name1 = name1;
        this.name2 = name2;
    }

    public int streckenId;
    public long myDBNodeId;
    public long sectionId;
    public long elementId;
    public long km;
    public String type;
    public String ds100;
    public String stationName;
    public String name1;
    public String name2;
}
