package main.java.OSM;

import com.slimjars.dist.gnu.trove.list.TLongList;
import main.java.OSM.OSMNode;

import java.util.List;
import java.util.Map;

public class OSMWay {
    public OSMWay(TLongList nodesOSMId, List<OSMNode> nodes, long osmId, long myId, Map<String, String> tags) {
        this.nodesOSMId = nodesOSMId;
        this.nodes = nodes;
        this.osmId = osmId;
        this.myId = myId;
        this.tags = tags;
    }
    public TLongList nodesOSMId;
    public List<OSMNode> nodes;
    public long osmId;
    public long myId;
    public Map<String, String> tags;
}
