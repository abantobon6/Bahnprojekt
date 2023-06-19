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
            System.out.println("Starting DB...");
            readOSM();
            System.out.println("Finished DB. Starting OSM...");
            readDB();
            System.out.println("Finished DB.");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        findFixpoints();
        System.out.println(fixpoints.size());
    }

    private static void readDB() {
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("C:/Users/Anton/IdeaProjects/Bahnprojekt/data/DB"));
            for(Path filePath : ds) {
                BufferedReader br = null;
                String line = "";
                br = new BufferedReader(new FileReader(filePath.toFile()));
                long ctr = 0;
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    dbNodes.add(new DBNode(ctr, Long.parseLong(tags[0]), Long.parseLong(tags[1]), Long.parseLong(tags[2]), tags[3], tags[4], tags[5], tags[6], tags[7]));
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

        OsmIterator iter = new OsmXmlIterator("C:/Users/Anton/IdeaProjects/Bahnprojekt/data/germany_railway.osm", false);
        for (EntityContainer container : iter) {
            if (container.getType() == EntityType.Node) {
                tmpNode = (Node) container.getEntity();
                osmNodes.add(new OSMNode(convertListToMap(tmpNode.getTags()), tmpNode.getId(), ctr, tmpNode.getLongitude(), tmpNode.getLongitude()));
            }

            if (container.getType() == EntityType.Way) {
                tmpWay = (Way) container.getEntity();
                osmWays.add(new OSMWay(tmpWay.getNodes(), null, tmpWay.getId(), ctr, convertListToMap(tmpWay.getTags())));
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
            if(osmNode.tags.containsValue("switch") && osmNode.tags.containsKey("ref"))
                osmSwitches.add(osmNode);
        }

        for(DBNode dbNode : dbNodes) {
            if(Objects.equals(dbNode.type, "simple_switch"))
                dbSwitches.add(dbNode);
        }

        for(OSMWay way : osmWays) {
            if(!way.tags.containsKey("ref"))
                continue;

            int wayRef = Integer.getInteger(way.tags.get("ref"));

            for(long nodeId : way.nodesId) {
                for (OSMNode osmNode_switch : osmSwitches) {
                    if(osmNode_switch.osmId == nodeId) {

                    }
                }
            }
        }
    }

}