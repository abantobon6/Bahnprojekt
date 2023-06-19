package main.java.OSM;

import java.util.Map;

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

}
