package main.java.OSM;

import java.util.Map;

public class OSMNode {
    public OSMNode(Map<String, String> tags, long osmId, long myId, double lon, double lat) {
        this.tags = tags;
        this.osmId = osmId;
        this.myId = myId;
        this.lon = lon;
        this.lat = lat;
    }
    public Map<String, String> tags;
    public long osmId;
    public long myId;
    public double lon;
    public double lat;

}
