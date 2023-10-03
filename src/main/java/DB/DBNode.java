package main.java.DB;

import java.util.ArrayList;
import java.util.List;

/**
 * Object that represents an DB node containing all its information.
 */
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

    public double lat = -1;
    public double lon = -1;

    /**
     * Returns a String containing all the information this object contains.
     *
     * @return String representation of concatenation of the attributes of this object
     */
    public String toString() {
        return "[" + streckenId + "; " +
                sectionId + "; " + elementId + "; " +
                km + "; " + type + "; " +
                ds100 + "; " + stationName + "; " +
                name1 + "; " + name2 + "; " + lat + "; " + lon +
                "; Neighbours: " + listToString(neighbours) + "]";
    }

    /**
     * Returns a String containing all the information given by the input data plus gps location.
     * String is the same design as a line of the input data.
     *
     * @return String representation of concatenation of the attributes of this object (without neighbours)
     */
    public String print() {
        return sectionId + ";" + elementId + ";" + km + ";" + type + ";" + ds100 + ";" +
                stationName + ";" + name1 + ";" + name2 + ";\'" + lat + ";\'" + lon;
    }


    /**
     * Returns the concatenation of the elements of a list transformed to Strings.
     *
     * @param list list
     * @return String representation of concatenation of the elements of list
     */
    private String listToString(List<Long> list) {
        String resultString = "";
        for (Long l : list) {
            resultString = resultString.concat(String.valueOf(l).concat(", "));
        }
        return resultString;
    }
}
