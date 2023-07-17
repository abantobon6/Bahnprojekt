package main.java.OSM;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class OSMNode {
    public OSMNode(Map<String, String> tags, long osmId, long myOSMNodeId, double lon, double lat) {
        this.tags = tags;
        this.osmId = osmId;
        this.myOSMNodeId = myOSMNodeId;
        this.lon = lon;
        this.lat = lat;
    }
    public Map<String, String> tags;
    public long osmId;
    public long myOSMNodeId;
    public double lon;
    public double lat;
    public HashSet<OSMWay> ways = new HashSet<>();

    public String toString() {
        return "[" + mapToString(tags)  + "; " + String.valueOf(osmId) + "; " +
                String.valueOf(myOSMNodeId) + "; " + String.valueOf(lon) + "; " + String.valueOf(lat) + "]";
    }

    public String print() {
        return "<node id=\""+ osmId +"\" version=\"10\" timestamp=\"2"+ LocalDateTime.now().toString() +"\" uid=\"0\" user=\"\" lat=\""+ lat +"\" lon=\""+ lon +"\">\n" +
                mapToString(tags);
    }

    public List<OSMNode> getNeigbours() {
        List<OSMNode> neighbours = new ArrayList<>();
        for (OSMWay way : ways) {
            for (int i = 0; i < way.nodes.size(); i++) {
                if (way.nodes.get(i) != null && Objects.equals(this, way.nodes.get(i))) {
                    if (i > 0)
                        neighbours.add(way.nodes.get(i - 1));
                    if (i < way.nodes.size() - 1)
                        neighbours.add(way.nodes.get(i + 1));
                }
            }
        }
        return neighbours;
    }

    public List<OSMNode> getNeigboursWithout(OSMNode nodeToIgnore) {
        List<OSMNode> neighbours = new ArrayList<>();
        for (OSMWay way : ways) {
            for (int i = 0; i < way.nodes.size(); i++) {
                System.out.println("node: " + way.nodes.get(i) + " from way: " + way.myId);
                if (way.nodes.get(i) != null && Objects.equals(this, way.nodes.get(i))) {
                    if (i > 0 && !Objects.equals(nodeToIgnore, way.nodes.get(i-1))) {
                        System.out.println("NodeToIgnore: " + nodeToIgnore.osmId + "|" + way.nodes.get(i - 1));
                        neighbours.add(way.nodes.get(i - 1));
                    }
                    if (i < way.nodes.size() - 1 && !Objects.equals(nodeToIgnore, way.nodes.get(i+1))) {
                        System.out.println("NodeToIgnore: " + nodeToIgnore.osmId + "|" + way.nodes.get(i + 1));
                        neighbours.add(way.nodes.get(i + 1));
                    }
                }
            }
        }
        return neighbours;
    }

    private String mapToString(Map<String, String> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }
}
