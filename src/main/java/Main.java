package main.java;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;
import main.java.DB.DBNode;
import main.java.DB.DBSection;
import main.java.DB.DBWay;
import main.java.OSM.OSMNode;
import main.java.OSM.OSMWay;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static HashMap<Long, OSMNode> osmNodes = new HashMap<>();
    private static HashMap<Long,DBNode> dbNodes = new HashMap<>();
    private static HashMap<Long,OSMWay> osmWays = new HashMap<>();
    private static HashMap<Integer, DBWay> dbWays = new HashMap<>();
    private static HashMap<String, List<Long>> equalNodes = new HashMap<>();
    private static HashMap<Long, Long> fixpoints = new HashMap<>(); //dbNodes mapped to osmNodes
    private static Map<Long, DBNode> workDbNodes = new HashMap<>();

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Starting OSM...");
        readOSM();
        System.out.println("Finished OSM. Starting DB...");
        readDB();
        System.out.println("Finished DB.");
        //printDBInfo();
        //printOSMInfo();
        System.out.println("Starting search for fixpoints...");
        findFixpoints();
        System.out.println("Found " + fixpoints.size() + " fixpoints.");
        System.out.println("Start mapping nodes...");
        mapNodes();
        System.out.println("Finished!");
    }

    private static void printDBInfo() {
        int ctrSections = 0;
        System.out.println("DB Info");
        System.out.println();
        for (DBWay way : dbWays.values()) {
            ctrSections += way.dbSectionIds.size();
        }

        int ctr0 = 0;
        int ctr1 = 0;
        int ctr2 = 0;
        int ctr3 = 0;
        int ctr4 = 0;
        int ctrSw = 0;
        int ctrLc = 0;
        int ctrB = 0;
        int ctrBo = 0;

        for (DBNode dbNode : dbNodes.values()) {

            switch (dbNode.neighbours.size()) {
                case 0 : ctr0++; break;
                case 1 : ctr1++; break;
                case 2 : ctr2++; break;
                case 3 : ctr3++; break;
                case 4 : ctr4++; break;
            }
            switch (dbNode.type) {
                case "simple_switch" : ctrSw++; break;
                case "level_crossing" : ctrLc++; break;
                case "bumper" : ctrB++; break;
                case "border" : ctrBo++; break;
            }
        }
        System.out.println("Anzahl Ways: " + dbWays.size());
        System.out.println("Anzahl Sections: " + ctrSections);
        System.out.println("Anzahl Nodes: " + dbNodes.size());
        System.out.println("Anzahl Nodes mit 0 Nachbarn: " + ctr0);
        System.out.println("Anzahl Nodes mit 1 Nachbarn: " + ctr1);
        System.out.println("Anzahl Nodes mit 2 Nachbarn: " + ctr2);
        System.out.println("Anzahl Nodes mit 3 Nachbarn: " + ctr3);
        System.out.println("Anzahl Nodes mit 4 Nachbarn: " + ctr4);
        System.out.println("Anzahl Weichen: " + ctrSw);
        System.out.println("Anzahl Kreuzungen: " + ctrLc);
        System.out.println("Anzahl Prellböcke: " + ctrB);
        System.out.println("Anzahl Borders: " + ctrBo);

        System.out.println();
    }

    private static void printOSMInfo() {
        System.out.println("OSM Info:");
        System.out.println();

        int ctr0 = 0;
        int ctr1 = 0;
        int ctr2 = 0;
        int ctr3 = 0;
        int ctr4 = 0;

        for (OSMNode osmNode : osmNodes.values()) {

            switch (osmNode.neighbours.size()) {
                case 0 : ctr0++; break;
                case 1 : ctr1++; break;
                case 2 : ctr2++; break;
                case 3 : ctr3++; break;
                case 4 : ctr4++; break;
            }
        }

        System.out.println("Anzahl Nodes: " + osmNodes.size());
        System.out.println("Anzahl Nodes mit 0 Nachbarn: " + ctr0);
        System.out.println("Anzahl Nodes mit 1 Nachbarn: " + ctr1);
        System.out.println("Anzahl Nodes mit 2 Nachbarn: " + ctr2);
        System.out.println("Anzahl Nodes mit 3 Nachbarn: " + ctr3);
        System.out.println("Anzahl Nodes mit 4 Nachbarn: " + ctr4);

        System.out.println();
    }
    private static void readDB() {
        //read files
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("data/DB"));
            for (Path filePath : ds) {
                int streckenId = Integer.parseInt(filePath.toString().substring(8, 12));

                List<DBSection> sections = new ArrayList<>();
                DBSection newSection = new DBSection(0, new ArrayList<>());

                BufferedReader br = null;
                String line = "";
                br = new BufferedReader(new FileReader(filePath.toFile()));
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    DBNode node = new DBNode(Long.parseLong(tags[0].concat(tags[1])), streckenId, Long.parseLong(tags[0]), Long.parseLong(tags[1]), Long.parseLong(tags[2]), tags[3], tags[4], tags[5], tags[6], tags[7]);
                    dbNodes.put(node.nodeId, node);
                    workDbNodes.put(node.nodeId, node);

                    if (sections.stream().noneMatch(x -> x.sectionId == node.sectionId)) {
                        sections.add(newSection);
                        newSection = new DBSection(node.sectionId, new ArrayList<>());
                        sections.add(new DBSection(node.sectionId, new ArrayList<>()));
                    }
                    newSection.nodes.add(node.nodeId);
                }
                sections.add(newSection);
                for (int i = 0; i < sections.size(); i++) {
                    if (sections.get(i).nodes.size() == 0)
                        sections.remove(i);
                }
                dbWays.put(streckenId, new DBWay(streckenId, sections));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //set neighbours
        for (DBWay dbWay : dbWays.values()) {
            for (DBSection dbSection : dbWay.dbSectionIds.stream().filter(x -> x.nodes.size() > 0).toList()) {
                DBNode prev = dbNodes.get(dbSection.nodes.get(0));
                for (int i = 1; i < dbSection.nodes.size(); i++) {
                    DBNode dbNode = dbNodes.get(dbSection.nodes.get(i));

                    prev.neighbours.add(dbNode.nodeId);
                    dbNode.neighbours.add((prev.nodeId));
                    dbNodes.put(dbSection.nodes.get(i-1), prev);
                    dbNodes.put(dbSection.nodes.get(i), dbNode);
                    workDbNodes.put(dbSection.nodes.get(i-1), prev);
                    workDbNodes.put(dbSection.nodes.get(i), dbNode);

                    prev = dbNode;
                }
            }
        }

        for (DBNode dbNode : dbNodes.values()) {
            String key = String.valueOf(dbNode.elementId).concat(dbNode.type).concat(dbNode.ds100).concat(dbNode.stationName).concat(dbNode.name1);
            if (equalNodes.containsKey(key)) {
                List<Long> listToUpdate = equalNodes.get(key);
                listToUpdate.add(dbNode.nodeId);
                equalNodes.put(key, listToUpdate);
            } else {
                List<Long> newList = new ArrayList<>();
                newList.add(dbNode.nodeId);
                equalNodes.put(key, newList);
            }
        }
        for (List<Long> nodes : equalNodes.values()) {
            List<Long> neighbours = new ArrayList<>();
            for (Long nodeId : nodes) {
                neighbours.addAll(dbNodes.get(nodeId).neighbours);
            }
            for (Long nodeId : nodes) {
                DBNode dbNodeToUpdate = dbNodes.get(nodeId);
                dbNodeToUpdate.setNeighbours(neighbours);
                dbNodes.put(nodeId, dbNodeToUpdate);
                workDbNodes.put(nodeId, dbNodeToUpdate);
            }
        }
    }

    private static void readOSM() throws FileNotFoundException {
        Node tmpNode;
        Way tmpWay;

        OsmIterator iter = new OsmXmlIterator("data/germany_railway.osm", false);
        for (EntityContainer container : iter) {
            if (container.getType() == EntityType.Node) {
                tmpNode = (Node) container.getEntity();
                osmNodes.put(tmpNode.getId(), new OSMNode(convertListToMap(tmpNode.getTags()), tmpNode.getId(), tmpNode.getLongitude(), tmpNode.getLatitude()));
            }

            if (container.getType() == EntityType.Way) {
                tmpWay = (Way) container.getEntity();
                List<OSMNode> nodes = new ArrayList<>();
                long prev_nodeId_Way = -4567;
                for (long nodeId_way : tmpWay.getNodes().toArray()) {
                    //set neighbours
                    if (prev_nodeId_Way != -4567 && osmNodes.containsKey(nodeId_way) && osmNodes.containsKey(prev_nodeId_Way)) {
                        OSMNode tempOsmNode = osmNodes.get(nodeId_way);
                        tempOsmNode.neighbours.add(prev_nodeId_Way);
                        osmNodes.put(nodeId_way, tempOsmNode);
                        tempOsmNode = osmNodes.get(prev_nodeId_Way);
                        tempOsmNode.neighbours.add(nodeId_way);
                        osmNodes.put(prev_nodeId_Way, tempOsmNode);
                    }
                    prev_nodeId_Way = nodeId_way;

                    //füge passende Nodes zu Liste hinzu
                    if (osmNodes.containsKey(nodeId_way))
                        nodes.add(osmNodes.get(nodeId_way));
                }
                //füge Ways hinzu
                OSMWay newOSMWay = new OSMWay(tmpWay.getNodes(), nodes, tmpWay.getId(), convertListToMap(tmpWay.getTags()));
                osmWays.put(tmpWay.getId(), newOSMWay);
            }
        }
    }

    private static void findFixpoints() {
        List<DBNode> dbSwitches = dbNodes.values().stream().filter(x -> Objects.equals(x.type, "simple_switch")
                                                                    || Objects.equals(x.type, "cross")
                                                                    || Objects.equals(x.type, "ms")).toList();
        for (OSMWay way : osmWays.values()) {
            if (!way.tags.containsKey("ref"))
                continue;
            String wayRef = way.tags.get("ref");
            for (OSMNode osmNode : way.nodes) {
                if (osmNode.tags.containsKey("ref") &&
                        (osmNode.tags.containsValue("switch") || osmNode.tags.containsValue("signal:main") || osmNode.tags.containsValue("railway_crossing"))) {
                    for (DBNode dbSwitch : dbSwitches) {
                        if (Objects.equals(String.valueOf(dbSwitch.streckenId), wayRef) &&
                                (Objects.equals(osmNode.tags.get("ref"), dbSwitch.name1) || Objects.equals(osmNode.tags.get("ref"), dbSwitch.name2))) {
                            DBNode tempDbNode = workDbNodes.get(dbSwitch.nodeId);
                            tempDbNode.mapped = true;
                            workDbNodes.put(dbSwitch.nodeId, tempDbNode);

                            fixpoints.put(dbSwitch.nodeId, osmNode.osmId);
                            dbSwitch.lat = osmNode.lat;
                            dbSwitch.lon = osmNode.lon;
                            dbNodes.put(dbSwitch.nodeId, dbSwitch);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, String> convertListToMap(List<? extends OsmTag> list) {
        Map<String, String> map = new HashMap<>();
        for (OsmTag tag : list) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    public static void mapNodes() {

        while (true) {
            DBNode startNode = dbNodes.get(fixpoints.keySet().stream().toList().get(new Random().nextInt(fixpoints.size())));
            ArrayList<Long> nodesToCheck = new ArrayList<>(startNode.neighbours);
            ArrayList<Long> updatedNodesToCheck = nodesToCheck;

            HashMap<Long, Long> dbNeighbourNet = new HashMap<>();
            HashMap<Long, Long> visitedNodes = new HashMap<>();
            for (Long neighbourId : nodesToCheck) {
                dbNeighbourNet.put(neighbourId, startNode.nodeId);
            }

            boolean cont = true;

            while (cont) {
                nodesToCheck = (ArrayList<Long>) updatedNodesToCheck.clone();
                updatedNodesToCheck = new ArrayList<>();
                for (Long neighbourId : nodesToCheck) {

                    if (fixpoints.get(neighbourId) != null && dbNodes.get(neighbourId).elementId != startNode.elementId) {
                        System.out.println("A: " + startNode.nodeId + ", B: " + neighbourId);
                        //get dbPath
                        List<Long> dbPath = new ArrayList<>();
                        dbPath.add(neighbourId);
                        Long tempId = neighbourId;
                        while (tempId != null && tempId != startNode.nodeId) {
                            dbPath.add(dbNeighbourNet.get(tempId));
                            tempId = dbNeighbourNet.get(tempId);
                        }
                        System.out.println("DB-Path: " + dbPath.size());
                        mapPath(startNode, dbNodes.get(neighbourId), dbPath);
                        cont = false;
                        break;
                    }
                    visitedNodes.put(neighbourId, neighbourId);
                    for (Long id : dbNodes.get(neighbourId).neighbours) {
                        if (visitedNodes.get(id) == null) {
                            updatedNodesToCheck.add(id);
                            dbNeighbourNet.put(id, neighbourId);
                        }
                    }
                }
                System.out.println("Mapped nodes: " + dbNodes.values().stream().filter(x -> x.lat != -1).toList().size());
            }
        }
    }

    private static void mapPath(DBNode startNode, DBNode endNode, List<Long> dbPath) {
        //get related osmPath
        OSMNode osmStartNode = osmNodes.get(fixpoints.get(startNode.nodeId));
        OSMNode osmEndNode = osmNodes.get(fixpoints.get(endNode.nodeId));

        System.out.println("OSM-StartNode: " + osmStartNode.osmId);
        System.out.println("OSM-EndNode: " + osmEndNode.osmId);

        ArrayList<Long> osmPath = new ArrayList<>();
        ArrayList<Long> nodesToCheck;
        ArrayList<Long> updatedNodesToCheck = (ArrayList<Long>) osmEndNode.neighbours.clone();

        HashMap<Long, Long> visitedNodes = new HashMap<>();
        HashMap<Long, Long> osmNeighbourNet = new HashMap<>();
        for (Long id : updatedNodesToCheck) {
            osmNeighbourNet.put(id, osmEndNode.osmId);
        }
        boolean cont = true;
        while (cont) {
            nodesToCheck = (ArrayList<Long>) updatedNodesToCheck.clone();
            for (Long osmNodeId : nodesToCheck) {
                if (osmNodeId == osmStartNode.osmId) {
                    //get osmPath
                    osmPath.add(osmNodeId);
                    Long tempId = osmNodeId;
                    while (tempId != null && tempId != osmEndNode.osmId) {
                        osmPath.add(osmNeighbourNet.get(tempId));
                        tempId = osmNeighbourNet.get(tempId);
                    }
                    cont = false;
                    break;
                }
                for (Long id : osmNodes.get(osmNodeId).neighbours) {
                    if (visitedNodes.get(id) == null)
                        osmNeighbourNet.put(id, osmNodeId);
                }
                visitedNodes.put(osmNodeId, osmNodeId);
                updatedNodesToCheck.addAll(osmNodes.get(osmNodeId).neighbours.stream().filter(x -> visitedNodes.get(x) == null).toList());
            }
        }

        //map nodes in paths
        OSMNode nodeBefore = null;
        OSMNode nodeAfter = null;
        int breakIndex = 0;
        for (int i = 0; i < dbPath.size()-1; i++) {
            DBNode dbNode = dbNodes.get(dbPath.get(i));
            DBNode nextDbNode = dbNodes.get(dbPath.get(i+1));
            String key = String.valueOf(nextDbNode.elementId).concat(nextDbNode.type).concat(nextDbNode.ds100).concat(nextDbNode.stationName).concat(nextDbNode.name1);
            double dbNodeDist = Math.abs(dbNode.km - dbNodes.get(equalNodes.get(key).stream().filter(x -> dbNodes.get(x).sectionId == nextDbNode.sectionId).toList().get(0)).km);

            double osmNodeDist = 0;
            double lastOsmDist = 0;
            System.out.println("OSM-Path " + osmPath.size());
            for (int j = breakIndex; j < osmPath.size()-1; j++) {
                OSMNode osmNode = osmNodes.get(osmPath.get(j));
                OSMNode nextOsmNode = osmNodes.get(osmPath.get(j + 1));
                lastOsmDist = getDistance(osmNode.lat, osmNode.lon, nextOsmNode.lat, nextOsmNode.lon);
                osmNodeDist += lastOsmDist;
                if (dbNodeDist < osmNodeDist) {
                    nodeBefore = osmNode;
                    nodeAfter = nextOsmNode;
                    breakIndex = j+1;
                    break;
                }
            }
            //get gps-tag for dbNode
            setGpsTag(dbNode, nodeAfter, nodeBefore, (osmNodeDist - dbNodeDist) / lastOsmDist);
        }
    }

    private static void setGpsTag(DBNode dbNode, OSMNode nodeOne, OSMNode nodeTwo, double dist) {
        dbNode.lat = nodeOne.lat + (nodeTwo.lat - nodeOne.lat) * dist;
        dbNode.lon = nodeOne.lon + (nodeTwo.lon - nodeOne.lon) * dist;
        dbNodes.put(dbNode.nodeId, dbNode);
    }

    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        int radius = 6371;

        double lat = Math.toRadians(lat2 - lat1);
        double lon = Math.toRadians(lon2- lon1);

        double a = Math.sin(lat / 2) * Math.sin(lat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lon / 2) * Math.sin(lon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = radius * c;

        return Math.abs(d);
    }
}