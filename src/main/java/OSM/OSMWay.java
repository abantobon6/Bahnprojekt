package main.java.OSM;

import com.slimjars.dist.gnu.trove.list.TLongList;
import main.java.OSM.OSMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Object that represents an OSM way containing all its information.
 */
public class OSMWay {
    public OSMWay(TLongList nodesOSMId, List<OSMNode> nodes, long osmId, Map<String, String> tags) {
        this.nodesOSMId = nodesOSMId;
        this.nodes = nodes;
        this.osmId = osmId;
        this.tags = tags;
    }
    public TLongList nodesOSMId;
    public List<OSMNode> nodes = new ArrayList<>();
    public long osmId;
    public long myId;
    public Map<String, String> tags;
}
