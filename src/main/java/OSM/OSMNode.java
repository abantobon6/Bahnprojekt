package main.java.OSM;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public HashSet<OSMWay> ways;

    public String toString() {
        return "[" + mapToString(tags)  + "; " + String.valueOf(osmId) + "; " +
                String.valueOf(myOSMNodeId) + "; " + String.valueOf(lon) + "; " + String.valueOf(lat) + "]";
    }

    public List<OSMNode> getNeigbours() {
        List<OSMNode> neighbours = new ArrayList<>();
        for (OSMWay way : ways) {
            for (OSMNode node : way.nodes) {
                if (osmId == node.osmId) {
                    if (way.nodes.indexOf(node) > 0)
                        neighbours.add(way.nodes.get(way.nodes.indexOf(node) - 1));
                    if (way.nodes.indexOf(node) < way.nodes.size() - 1)
                        neighbours.add(way.nodes.get(way.nodes.indexOf(node) + 1));
                }
            }
        }
        return neighbours;
    }

    public List<OSMNode> getNeigboursWithout(OSMNode nodeToIgnore) {
        List<OSMNode> neighbours = new ArrayList<>();
        for (OSMWay way : ways) {
            for (OSMNode node : way.nodes) {
                if (osmId == node.osmId && nodeToIgnore.osmId != node.osmId) {
                    if (way.nodes.indexOf(node) > 0)
                        neighbours.add(way.nodes.get(way.nodes.indexOf(node) - 1));
                    if (way.nodes.indexOf(node) < way.nodes.size() - 1)
                        neighbours.add(way.nodes.get(way.nodes.indexOf(node) + 1));
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
