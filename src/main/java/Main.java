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

public class Main {

    private static HashMap<Long, OSMNode> osmNodes = new HashMap<>();
    private static HashMap<Long,DBNode> dbNodes = new HashMap<>();
    private static HashMap<Long,OSMWay> osmWays = new HashMap<>();
    private static HashMap<Integer, DBWay> dbWays = new HashMap<>();
    private static HashMap<Long, List<Long>> equalNodes = new HashMap<>();
    private static HashMap<Long, Long> anchorNodes = new HashMap<>(); //dbNodes mapped to osmNodes
    private static ArrayList<DBNode> mappedNodes = new ArrayList<>();
    private static File mappedNodesFile = new File("data/mappedNodes.txt");
    private static FileWriter writer;

    static {
        try {
            writer = new FileWriter(mappedNodesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Starting OSM...");
        readOSM();
        System.out.println("Finished OSM. Starting DB...");
        readDB();
        System.out.println("Finished DB.");
        printDBInfo();
        printOSMInfo();
        System.out.println("Starting search for anchorNodes" + "...");
        findAnchorNodes();
        System.out.println("Found " + anchorNodes.size() + " anchorNodes" + ".");
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
                case "cross" : ctrLc++; break;
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
                DBNode prev;
                DBNode dbNode;
                for (int i = 0; i < dbSection.nodes.size()-1; i++) {
                    prev = dbNodes.get(dbSection.nodes.get(i));
                    dbNode = dbNodes.get(dbSection.nodes.get(i+1));

                    prev.neighbours.add(dbNode.nodeId);
                    dbNode.neighbours.add((prev.nodeId));
                    dbNodes.put(prev.nodeId, prev);
                    dbNodes.put(dbNode.nodeId, dbNode);
                }
            }
        }
        for (DBNode dbNode : dbNodes.values()) {
            if (equalNodes.containsKey(dbNode.elementId)) {
                List<Long> listToUpdate = equalNodes.get(dbNode.elementId);
                listToUpdate.add(dbNode.nodeId);
                equalNodes.put(dbNode.elementId, listToUpdate);
            } else {
                List<Long> newList = new ArrayList<>();
                newList.add(dbNode.nodeId);
                equalNodes.put(dbNode.elementId, newList);
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

    private static void findAnchorNodes() {
        HashMap<Long, Long> unambiguousNodesDB = new HashMap<>();
        HashMap<Long, Long> unambiguousNodesOSM = new HashMap<>();
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
                            if (unambiguousNodesDB.get(dbSwitch.nodeId) != null || unambiguousNodesOSM.get(osmNode.osmId) != null) {
                                anchorNodes.remove(dbSwitch.nodeId);
                                dbSwitch.lat = -1;
                                dbSwitch.lon = -1;
                                dbNodes.put(dbSwitch.nodeId, dbSwitch);
                                continue;
                            }
                            unambiguousNodesDB.put(dbSwitch.nodeId, dbSwitch.nodeId);
                            unambiguousNodesOSM.put(osmNode.osmId, osmNode.osmId);

                            anchorNodes.put(dbSwitch.nodeId, osmNode.osmId);
                            dbSwitch.lat = osmNode.lat;
                            dbSwitch.lon = osmNode.lon;
                            dbNodes.put(dbSwitch.nodeId, dbSwitch);
                            mappedNodes.add(dbSwitch);
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
        while (!anchorNodes.isEmpty()) {
            HashMap<Long, Long> copyOfAnchorNodes = (HashMap<Long, Long>) anchorNodes.clone();
            for (Long idToRemove : copyOfAnchorNodes.keySet())
                if (dbNodes.get(idToRemove).neighbours.stream().allMatch(x -> dbNodes.get(x).lat != -1)) anchorNodes.remove(idToRemove);

            DBNode startNode = dbNodes.get(anchorNodes.keySet().stream().toList().get(new Random().nextInt(anchorNodes.size())));
            ArrayList<Long> nodesToCheck = new ArrayList<>(startNode.neighbours.stream().filter(x -> dbNodes.get(x).lat == -1).toList());
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
                    if (anchorNodes.get(neighbourId) != null && dbNodes.get(neighbourId).elementId != startNode.elementId) {
                        //get dbPath
                        LinkedList<Long> dbPath = new LinkedList<>();
                        int dbPathLength = 0;

                        dbPath.add(neighbourId);
                        Long tempId = neighbourId;
                        while (tempId != startNode.nodeId) {
                            DBNode lastNodeInDBPath = dbNodes.get(dbPath.getLast());
                            DBNode nodeToAddNext = dbNodes.get(dbNeighbourNet.get(tempId));
                            //get nodes with the same sectionId
                            for (Long nodeOneVariation : equalNodes.get(lastNodeInDBPath.elementId)) {
                                for (Long nodeTwoVariation : equalNodes.get(nodeToAddNext.elementId)) {
                                    if (dbNodes.get(nodeOneVariation).sectionId == dbNodes.get(nodeTwoVariation).sectionId)
                                        dbPathLength += Math.abs(dbNodes.get(nodeOneVariation).km - dbNodes.get(nodeTwoVariation).km);
                                }
                            }

                            dbPath.add(dbNeighbourNet.get(tempId));
                            tempId = dbNeighbourNet.get(tempId);
                        }
                        if (dbPath.size() > 2) {
                            mapPath(dbNodes.get(neighbourId), startNode, dbPath, dbPathLength);
                            System.out.println("Mapped nodes: " + dbNodes.values().stream().filter(x -> x.lat != -1).toList().size());
                            System.out.println("Size of anchorNodes" + ": " + anchorNodes.size());
                        }
                        cont = false;
                        break;
                    }
                    visitedNodes.put(dbNodes.get(neighbourId).elementId, dbNodes.get(neighbourId).elementId);
                    for (Long id : dbNodes.get(neighbourId).neighbours) {
                        if (visitedNodes.get(dbNodes.get(id).elementId) == null) {
                            updatedNodesToCheck.add(id);
                            dbNeighbourNet.put(id, neighbourId);
                        }
                    }
                }
            }
        }
    }

    private static void mapPath(DBNode startNode, DBNode endNode, List<Long> dbPath, double dbPathLength) {
        //get related osmPath
        OSMNode osmStartNode = osmNodes.get(anchorNodes.get(startNode.nodeId));
        OSMNode osmEndNode = osmNodes.get(anchorNodes.get(endNode.nodeId));

        ArrayList<Long> osmPath = new ArrayList<>();
        HashMap<Long, Double> nodesToCheck;
        HashMap<Long, Double> updatedNodesToCheck = new HashMap<>();
        for (Long nId : osmEndNode.neighbours) {
            updatedNodesToCheck.put(nId, getDistance(osmEndNode.lat, osmEndNode.lon, osmNodes.get(nId).lat, osmNodes.get(nId).lon) * 1000.0);
        }

        HashMap<Long, Long> visitedNodes = new HashMap<>();
        HashMap<Long, Long> osmNeighbourNet = new HashMap<>();
        for (Long id : updatedNodesToCheck.keySet()) {
            osmNeighbourNet.put(id, osmEndNode.osmId);
        }
        boolean cont = true;
        while (cont) {
            nodesToCheck = (HashMap<Long, Double>) updatedNodesToCheck.clone();
            for (Long osmNodeId : nodesToCheck.keySet()) {
                if (osmNodeId == osmStartNode.osmId) {
                    if (nodesToCheck.get(osmNodeId) < dbPathLength * 1.01 && nodesToCheck.get(osmNodeId) > dbPathLength * 0.99) {
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
                    else {
                        return;
                    }
                }
                for (Long id : osmNodes.get(osmNodeId).neighbours) {
                    if (visitedNodes.get(id) == null)
                        osmNeighbourNet.put(id, osmNodeId);
                }
                if (osmNodeId != osmStartNode.osmId)
                    visitedNodes.put(osmNodeId, osmNodeId);
                for (Long nId : osmNodes.get(osmNodeId).neighbours.stream().filter(x -> visitedNodes.get(x) == null).toList()) {
                    double osmPathLengthSoFar = nodesToCheck.get(osmNodeId) + getDistance(osmNodes.get(osmNodeId).lat, osmNodes.get(osmNodeId).lon, osmNodes.get(nId).lat, osmNodes.get(nId).lon) * 1000.0;
                    updatedNodesToCheck.put(nId, osmPathLengthSoFar);
                }
            }
        }

        try {
            writer.write("\n DB-start and DB-end: " + startNode + "; " + endNode + "\n OSM-start and osm-end: " + osmStartNode + "; " + osmEndNode + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //map nodes in paths
        OSMNode nodeBefore = null;
        OSMNode nodeAfter = null;
        int dbNodeDist = 0;
        int osmNodeDist = 0;
        Long nearestOSMNode = 0L;

        int breakIndex = 0;
        for (int i = 0; i < dbPath.size()-2; i++) {
            DBNode dbNode = dbNodes.get(dbPath.get(i));
            DBNode nextDbNode = dbNodes.get(dbPath.get(i+1));

            //update dbNodeDist
            double lastDbDist = 0;
            for (Long nodeOneVariation : equalNodes.get(dbNode.elementId)) {
                for (Long nodeTwoVariation : equalNodes.get(nextDbNode.elementId)) {
                    if (dbNodes.get(nodeOneVariation).sectionId == dbNodes.get(nodeTwoVariation).sectionId)
                        lastDbDist = Math.abs(dbNodes.get(nodeOneVariation).km - dbNodes.get(nodeTwoVariation).km);
                }
            }

            if (lastDbDist == 0) {
                nextDbNode.lat = dbNode.lat;
                nextDbNode.lon = dbNode.lon;
                dbNodes.put(nextDbNode.nodeId, nextDbNode);
                if (i==0)
                    nearestOSMNode = osmStartNode.osmId;
                if (anchorNodes.get(nextDbNode.nodeId) == null
                        && (Objects.equals(nextDbNode.type, "simple_switch") || Objects.equals(nextDbNode.type, "cross"))
                        && (osmNodes.get(nearestOSMNode).tags.containsValue("switch") || osmNodes.get(nearestOSMNode).tags.containsValue("railway_crossing")))
                    anchorNodes.put(nextDbNode.nodeId, nearestOSMNode);
                continue;
            }

            dbNodeDist += lastDbDist;
            double lastOsmDist = 0;
            for (int j = breakIndex; j < osmPath.size()-1; j++) {
                OSMNode osmNode = osmNodes.get(osmPath.get(j));
                OSMNode nextOsmNode = osmNodes.get(osmPath.get(j + 1));
                //update osmNodeDist
                lastOsmDist = getDistance(osmNode.lat, osmNode.lon, nextOsmNode.lat, nextOsmNode.lon) * 1000.0;
                osmNodeDist += lastOsmDist;
                if (dbNodeDist <= osmNodeDist || i == dbPath.size()-3) {
                    nodeBefore = osmNode;
                    nodeAfter = nextOsmNode;
                    breakIndex = j+1;
                    break;
                }
            }
            //if switch -> add to anchorNodes
            nearestOSMNode = (osmNodeDist - dbNodeDist < lastOsmDist - (osmNodeDist - dbNodeDist) ? nodeAfter.osmId : nodeBefore.osmId);
            if (anchorNodes.get(nextDbNode.nodeId) == null && (Objects.equals(nextDbNode.type, "simple_switch") || Objects.equals(nextDbNode.type, "cross"))) {
                if (osmNodes.get(nearestOSMNode).tags.containsValue("switch") || osmNodes.get(nearestOSMNode).tags.containsValue("railway_crossing"))
                    anchorNodes.put(nextDbNode.nodeId, nearestOSMNode);
            }
            //get gps-tag for dbNode
            if (dbNodes.get(nextDbNode.nodeId).lat == -1) {
                setGpsTag(nextDbNode, nodeAfter, nodeBefore, osmNodeDist - dbNodeDist);
            }
        }
        System.out.println("dbPathLength: " + dbPathLength);
        System.out.println("osmPathLength: " + osmNodeDist);
    }

    private static void setGpsTag(DBNode dbNode, OSMNode nodeOne, OSMNode nodeTwo, int dist) {
        dbNode.lat = nodeOne.lat + (nodeTwo.lat - nodeOne.lat) * (dist/1000.0);
        dbNode.lon = nodeOne.lon + (nodeTwo.lon - nodeOne.lon) * (dist/1000.0);
        dbNodes.put(dbNode.nodeId, dbNode);
        mappedNodes.add(dbNode);
        try {
            writer.write(dbNode.toString() + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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