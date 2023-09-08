package main.java.OSM;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        return "[" + mapToString(tags)  + "; " + osmId + "; " + lat + "; " +
                lon + "; Neighbours: " + listToString(neighbours) + "]";
    }

    private String listToString(List<Long> list) {
        String resultString = "";
        for (Long l : list) {
            resultString = resultString.concat(String.valueOf(l).concat(", "));
        }
        return resultString;
    }

    public String print() {
        return "<node id=\""+ osmId +"\" version=\"10\" timestamp=\"2"+ LocalDateTime.now().toString() +"\" uid=\"0\" user=\"\" lat=\""+ lat +"\" lon=\""+ lon +"\">\n" +
                mapToString(tags);
    }

    private String mapToString(Map<String, String> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }
}
