package main.java.DB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DBNode {

    public DBNode(long nodeId, int streckenId, long sectionId, long elementId, long km,String type, String ds100, String stationName, String name1, String name2) {
        this.nodeId = nodeId;
        this.streckenId = streckenId;
        this.sectionId = sectionId;
        this. elementId = elementId;
        this.km = km;
        this.type = type;
        this.ds100 = ds100;
        this.stationName = stationName;
        this.name1 = name1;
        this.name2 = name2;
    }

    public long nodeId;
    public int streckenId;
    public long sectionId;
    public long elementId;
    public long km;
    public String type;
    public String ds100;
    public String stationName;
    public String name1;
    public String name2;

    public List<Long> neighbours = new ArrayList<>();

    public boolean mapped = false;

    public double lat = -1;
    public double lon = -1;

    public String toString() {
        return "[" + streckenId + "; " +
                sectionId + "; " + elementId + "; " +
                km + "; " + type + "; " +
                ds100 + "; " + stationName + "; " +
                name1 + "; " + name2 + "; " + lat + "; " + lon + "; " + neighbours.size() + "; " +
                "; Neighbours: " + listToString(neighbours) + "]";
    }

    private String listToString(List<Long> list) {
        String resultString = "";
        for (Long l : list) {
            resultString = resultString.concat(String.valueOf(l).concat(", "));
        }
        return resultString;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }
    public void setStreckenId(int streckenId) {
        this.streckenId = streckenId;
    }
    public void setSectionId(long sectionId) {
        this.sectionId = sectionId;
    }
    public void setElementId(long elementId) {
        this.elementId = elementId;
    }
    public void setKm(long km) {
        this.km = km;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setDs100(String ds100) {
        this.ds100 = ds100;
    }
    public void setStationName(String stationName) {
        this.stationName = stationName;
    }
    public void setName1(String name1) {
        this.name1 = name1;
    }
    public void setName2(String name2) {
        this.name2 = name2;
    }
    public void setNeighbours(List<Long> neighbours) {
        this.neighbours = neighbours;
    }
    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }
}
