package main.java;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;
import main.java.DB.DBNode;
import main.java.DB.DBWay;
import main.java.OSM.OSMNode;
import main.java.OSM.OSMWay;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static List<OSMNode> osmNodes = new ArrayList<>();
    private static List<DBNode> dbNodes = new ArrayList<>();
    private static List<OSMWay> osmWays = new ArrayList<>();
    private static List<DBWay> dbWays = new ArrayList<>();
    private static List<Fixpoint> fixpoints = new ArrayList<>();
    private static List<OSMNode> resultNodes = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println("Starting OSM...");
            readOSM();
            System.out.println("Finished OSM. Starting DB...");
            readDB();
            System.out.println("Finished DB.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Starting search for fixpoints...");
        findFixpoints();
        System.out.println("Found " + fixpoints.size() + " fixpoints.");
    }

    private static void readDB() {
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("data/DB"));
            for(Path filePath : ds) {
                BufferedReader br = null;
                String line = "";
                br = new BufferedReader(new FileReader(filePath.toFile()));
                long ctr = 0;
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    dbNodes.add(new DBNode(Integer.parseInt(filePath.toString().substring(8, 12)), ctr, Long.parseLong(tags[0]), Long.parseLong(tags[1]), Long.parseLong(tags[2]), tags[3], tags[4], tags[5], tags[6], tags[7]));
                    ctr++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void readOSM() throws FileNotFoundException {

        Node tmpNode;
        Way tmpWay;
        long ctr = 0;

        OsmIterator iter = new OsmXmlIterator("data/germany_railway.osm", false);
        for (EntityContainer container : iter) {
            if (container.getType() == EntityType.Node) {
                tmpNode = (Node) container.getEntity();
                osmNodes.add(new OSMNode(convertListToMap(tmpNode.getTags()), tmpNode.getId(), ctr, tmpNode.getLongitude(), tmpNode.getLongitude()));
                System.out.println("Progress: Node " + ctr);
            }

            if (container.getType() == EntityType.Way) {
                tmpWay = (Way) container.getEntity();
                List<OSMNode> nodes = new ArrayList<>();
                for (long nodeId_way : tmpWay.getNodes().toArray()) {
                    for (OSMNode node : osmNodes) {
                        //füge passende Nodes zu Liste hinzu
                        if (node.osmId == nodeId_way)
                            nodes.add(node);
                    }
                }
                osmWays.add(new OSMWay(tmpWay.getNodes(), nodes, tmpWay.getId(), ctr, convertListToMap(tmpWay.getTags())));
                System.out.println("Progress: Way " + (ctr - osmNodes.size()));
            }
            ctr++;
        }
    }

    private static Map<String, String> convertListToMap(List<? extends OsmTag> list) {
        Map<String, String> map = new HashMap<>();
        for(OsmTag tag : list) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    private static void findFixpoints() {
        List<OSMNode> osmSwitches = new ArrayList<>();
        List<DBNode> dbSwitches = new ArrayList<>();

        for(OSMNode osmNode : osmNodes) {
            if(osmNode.tags.containsValue("switch") && osmNode.tags.containsKey("ref")) {
                //System.out.println("OSMSwitch: " + osmNode.osmId);
                osmSwitches.add(osmNode);
            }
        }

        for(DBNode dbNode : dbNodes) {
            if(Objects.equals(dbNode.type, "simple_switch")) {
                //System.out.println("DBSwitch: " + dbNode.elementId);
                dbSwitches.add(dbNode);
            }
        }
        System.out.println("B");
        for(OSMWay way : osmWays) {
            if(!way.tags.containsKey("ref"))
                continue;

            String wayRef = way.tags.get("ref");
            System.out.println(wayRef);

            for(OSMNode osmNode : way.nodes) {
                if(osmSwitches.contains(osmNode)) {
                    fixpoints.add(new Fixpoint(osmNode, getDBSwitch(dbSwitches, wayRef, osmNode.tags.get("ref"))));
                }
            }
        }
    }

    private static DBNode getDBSwitch(List<DBNode> dbSwitches, String wayRef, String ref) {
        for(DBNode dbSwitch : dbSwitches) {
            if(dbSwitch.streckenId == Integer.getInteger(wayRef) && Objects.equals(dbSwitch.name1, ref))
                return dbSwitch;
        }
        return null;
    }

}