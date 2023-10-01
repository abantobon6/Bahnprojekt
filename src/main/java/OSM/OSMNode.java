package main.java.OSM;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Object that represents an OSM node containing all its information.
 */
public class OSMNode {
    public OSMNode(Map<String, String> tags, long osmId, double lon, double lat) {
        this.tags = tags;
        this.osmId = osmId;
        this.lon = lon;
        this.lat = lat;
    }
    public Map<String, String> tags;
    public long osmId;
    public double lon;
    public double lat;
    public ArrayList<Long> neighbours = new ArrayList<>();

    public String toString() {
        return "[" + osmId + "; " + lat + "; " + lon + "; "
                + mapToString(tags) + "; Neighbours: " + listToString(neighbours) + "]";
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

    /**
     * Returns the concatenation of the values of a Map transformed to Strings.
     *
     * @param map map
     * @return String representation of concatenation of the values of map
     */
    private String mapToString(Map<String, String> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
