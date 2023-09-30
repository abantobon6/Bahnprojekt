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
import java.util.stream.Stream;

public class Main {

    //registered nodes from filtered OpenStreetMap-data
    private static HashMap<Long, OSMNode> osmNodes = new HashMap<>();
    //registered nodes from DB-track-plan
    private static HashMap<Long,DBNode> dbNodes = new HashMap<>();
    private static HashMap<Long,OSMWay> osmWays = new HashMap<>();
    private static HashMap<Integer, DBWay> dbWays = new HashMap<>();
    private static HashMap<Long, List<Long>> equalNodes = new HashMap<>();
    private static HashMap<String, Double[]> betriebsstellen = new HashMap<String, Double[]>();
    private static HashMap<Long, Long> anchorNodes = new HashMap<>(); //dbNodes mapped to osmNodes
    private static HashMap<Long, Long> openAnchorNodes = new HashMap<>();
    private static int countOfMissedPaths1 = 0;
    private static int countOfMissedPaths2 = 0;
    private static int countOfMissedPaths3 = 0;

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Starting OSM...");
        readOSM();
        System.out.println("Finished OSM. Starting DB...");
        readDB();
        readBetriebsstellenMapping();
        System.out.println("Finished DB.");
        System.out.println();
        printDBInfo();
        printOSMInfo();
        System.out.println("Starting search for anchorNodes" + "...");
        findAnchorNodes();
        System.out.println("Found " + anchorNodes.size() + " anchorNodes" + ".");
        System.out.println("Start mapping nodes...");
        mapNodes();
        System.out.println("Mapped " + dbNodes.values().stream().filter(x -> x.lat != -1).toList().size() + " nodes.");
        System.out.println("Start dumping...");
        dumpMappedNodes();
        System.out.println("Finished!");
    }

    private static void printDBInfo() {
        int ctrSections = 0;
        System.out.println("DB Info:");
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
        System.out.println();
        System.out.println("OSM Info:");

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

                String line;
                BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()));
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    DBNode node = new DBNode(Long.parseLong(tags[0].concat(tags[1])), streckenId, Long.parseLong(tags[0]), Long.parseLong(tags[1]), Long.parseLong(tags[2]), tags[3], tags[4], tags[5], tags[6], tags[7]);
                    dbNodes.put(node.nodeId, node);
                    equalNodes.putIfAbsent(node.elementId, new ArrayList<>());
                    equalNodes.get(node.elementId).add(node.nodeId);

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
                for (int i = 0; i < dbSection.nodes.size()-1; i++) {
                    dbNodes.get(dbSection.nodes.get(i)).neighbours.add(dbSection.nodes.get(i+1));
                    dbNodes.get(dbSection.nodes.get(i+1)).neighbours.add(dbSection.nodes.get(i));
                }
            }
        }
        for (List<Long> nodes : equalNodes.values()) {
            List<Long> neighbours = new ArrayList<>();
            for (Long nodeId : nodes) {
                neighbours.addAll(dbNodes.get(nodeId).neighbours);
            }
            for (Long nodeId : nodes) {
                dbNodes.get(nodeId).neighbours = neighbours;
            }
        }
    }

    private static void readBetriebsstellenMapping() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("data/betriebsstelleToGps.CSV"));
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] tags = line.split(";", -1);
                betriebsstellen.put(tags[0], new Double[] {Double.parseDouble(tags[1]), Double.parseDouble(tags[2])});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        osmNodes.get(nodeId_way).neighbours.add(prev_nodeId_Way);
                        osmNodes.get(prev_nodeId_Way).neighbours.add(nodeId_way);
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
        int ctrUnusedAnchorpoints = 0;
        List<DBNode> dbSwitches = equalNodes.values().stream().map(x -> dbNodes.get(x.get(0))).filter(y -> (Objects.equals(y.type, "simple_switch")
                                                                    || Objects.equals(y.type, "cross")
                                                                    || Objects.equals(y.type, "ms"))).toList();
        for (OSMWay way : osmWays.values()) {
            if (!way.tags.containsKey("ref"))
                continue;
            String wayRef = way.tags.get("ref");
            for (OSMNode osmNode : way.nodes) {
                if (osmNode.tags.containsKey("ref") &&
                        (osmNode.tags.containsValue("switch") || osmNode.tags.containsValue("signal:main") || osmNode.tags.containsValue("railway_crossing"))) {
                    ArrayList<DBNode> potentialDbNodes = new ArrayList<>();
                    ArrayList<Long> potentialDbElementIds = new ArrayList<>();
                    for (DBNode dbSwitch : dbSwitches) {
                        if (!potentialDbElementIds.contains(dbSwitch.elementId) && Objects.equals(String.valueOf(dbSwitch.streckenId), wayRef)
                                && (Objects.equals(osmNode.tags.get("ref"), dbSwitch.name1)
                                || Objects.equals(osmNode.tags.get("ref"), dbSwitch.name2)
                                || Objects.equals(osmNode.tags.get("ref"), "W" + dbSwitch.name1)
                                || Objects.equals(osmNode.tags.get("ref"), "W" + dbSwitch.name2))) {
                            potentialDbElementIds.add(dbSwitch.elementId);
                            potentialDbNodes.add(dbSwitch);
                        }
                    }
                    if (!potentialDbNodes.isEmpty()) {
                        DBNode minDistNode = potentialDbNodes.get(0);
                        if (potentialDbNodes.size() > 1) {
                            double minDist = Double.MAX_VALUE;
                            if (potentialDbNodes.stream().anyMatch(x -> betriebsstellen.get(x.ds100) == null)) {
                                ctrUnusedAnchorpoints += potentialDbNodes.size();
                                continue;
                            }
                            for (DBNode n : potentialDbNodes) {
                                double tempDist;
                                if ((tempDist = getDistance(osmNode.lat, osmNode.lon, betriebsstellen.get(n.ds100)[0], betriebsstellen.get(n.ds100)[1])) < minDist) {
                                    minDist = tempDist;
                                    minDistNode = n;
                                }
                            }
                        }
                        anchorNodes.put(minDistNode.nodeId, osmNode.osmId);
                        openAnchorNodes.put(minDistNode.nodeId, osmNode.osmId);
                        minDistNode.lat = osmNode.lat;
                        minDistNode.lon = osmNode.lon;
                        dbNodes.put(minDistNode.nodeId, minDistNode);
                    }
                }
            }
        }
        System.out.println("Unused anchornodes: " + ctrUnusedAnchorpoints);
    }

    private static Map<String, String> convertListToMap(List<? extends OsmTag> list) {
        Map<String, String> map = new HashMap<>();
        for (OsmTag tag : list) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    public static void mapNodes() {
        int ctr = 0;
        while (!openAnchorNodes.isEmpty()) {
            DBNode startNode = dbNodes.get(openAnchorNodes.keySet().stream().toList().get(new Random().nextInt(openAnchorNodes.size())));
            if (dbNodes.get(startNode.nodeId).neighbours.stream().allMatch(x -> dbNodes.get(x).lat != -1)) {
                openAnchorNodes.remove(startNode.nodeId);
                continue;
            }

            ArrayList<Long> nodesToCheck = new ArrayList<>(startNode.neighbours.stream().filter(x -> dbNodes.get(x).lat == -1).toList());
            ArrayList<Long> updatedNodesToCheck = nodesToCheck;

            HashMap<Long, Long> dbNeighbourNet = new HashMap<>();
            HashMap<Long, Long> visitedNodes = new HashMap<>();
            visitedNodes.put(startNode.elementId, 0L);
            for (Long neighbourId : nodesToCheck) {
                dbNeighbourNet.put(neighbourId, startNode.nodeId);
            }

            boolean cont = true;
            while (cont && !nodesToCheck.isEmpty()) {
                nodesToCheck = (ArrayList<Long>) updatedNodesToCheck.clone();
                updatedNodesToCheck = new ArrayList<>();
                for (Long neighbourId : nodesToCheck) {
                    if (anchorNodes.get(neighbourId) != null && dbNodes.get(neighbourId).elementId != startNode.elementId) {
                        //get dbPath
                        LinkedList<Long> dbPath = new LinkedList<>();
                        int dbPathLength = 0;

                        dbPath.add(neighbourId);
                        Long tempId = neighbourId;

                        double ctrSwitches = 0.0;
                        double ctrCrosses = 0.0;
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
                            if (Objects.equals(dbNodes.get(dbNeighbourNet.get(tempId)).type, "simple_switch"))
                                ctrSwitches++;
                            if (Objects.equals(dbNodes.get(dbNeighbourNet.get(tempId)).type, "cross"))
                                ctrCrosses++;
                            dbPath.add(dbNeighbourNet.get(tempId));
                            tempId = dbNeighbourNet.get(tempId);
                        }
                        Collections.reverse(dbPath);
                        mapPath(startNode, dbNodes.get(neighbourId), dbPath, dbPathLength, ctrSwitches, ctrCrosses);
                        if (ctr == 1000) {
                            System.out.println("Mapped nodes: " + dbNodes.values().stream().filter(x -> x.lat != -1).toList().size());
                            System.out.println("Size of OpenAnchorNodes" + ": " + openAnchorNodes.size());
                            System.out.println("Count of missed paths 0: " + countOfMissedPaths1);
                            System.out.println("Count of missed paths !=: " + countOfMissedPaths2);
                            countOfMissedPaths1 = 0;
                            countOfMissedPaths2 = 0;
                            ctr = 0;
                        }
                        ctr++;
                        cont = false;
                        break;
                    }
                    visitedNodes.put(dbNodes.get(neighbourId).elementId, 0L);
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

    private static void mapPath(DBNode startNode, DBNode endNode, List<Long> dbPath, double dbPathLength, double ctrDBSwitches, double ctrDBCrosses) {
        //get related osmPath
        OSMNode osmStartNode = osmNodes.get(openAnchorNodes.get(startNode.nodeId));
        OSMNode osmEndNode = osmNodes.get(anchorNodes.get(endNode.nodeId));
        double osmPathLength = 0.0;
        double viceOsmPathLength = 0.0;

        ArrayList<Long> osmPath = new ArrayList<>();
        ArrayList<Long>[] viceOsmPath = new ArrayList[]{new ArrayList<Long>(), new ArrayList<Long>(List.of(0L))};
        double bestPathLengthDif = Double.MAX_VALUE;
        HashMap<Long, Double[]> nodesToCheck;
        HashMap<Long, Double[]> updatedNodesToCheck = new HashMap<>();
        for (Long nId : osmStartNode.neighbours) {
            updatedNodesToCheck.put(nId,
                    new Double[]{getDistance(osmStartNode.lat, osmStartNode.lon, osmNodes.get(nId).lat, osmNodes.get(nId).lon) * 1000.0,
                            osmNodes.get(nId).tags.containsValue("switch") ? 1.0 : 0.0,
                            osmNodes.get(nId).tags.containsValue("railway_crossing") ? 1.0 : 0.0, 1.0});
        }

        HashMap<Long, List<Long>> osmNeighbourNet = new HashMap<>();
        for (Long id : updatedNodesToCheck.keySet()) {
            osmNeighbourNet.put(id, new ArrayList<>(List.of(osmStartNode.osmId)));
        }
        boolean found1 = false;
        double countOfDists1 = 0;
        double countOfDists2 = 0;
        boolean cont = true;
        while (cont && !updatedNodesToCheck.isEmpty()) {
            cont = false;
            nodesToCheck = (HashMap<Long, Double[]>) updatedNodesToCheck.clone();
            updatedNodesToCheck = new HashMap<>();
            for (Long osmNodeId : nodesToCheck.keySet()) {
                if (nodesToCheck.get(osmNodeId)[0] < dbPathLength)
                    cont = true;
                if ((osmNeighbourNet.get(osmNodeId) != null && osmNeighbourNet.get(osmNodeId).contains(osmNodeId))
                        || nodesToCheck.get(osmNodeId)[2] > ctrDBCrosses
                        || nodesToCheck.get(osmNodeId)[1] > ctrDBSwitches)
                    continue;
                if (osmNodeId == osmEndNode.osmId) {
                    //get osmPath
                    if (nodesToCheck.get(osmNodeId)[1] == ctrDBSwitches && nodesToCheck.get(osmNodeId)[2] == ctrDBCrosses) {
                        found1 = true;
                        if (bestPathLengthDif > Math.abs(dbPathLength - nodesToCheck.get(osmNodeId)[0])) {
                            bestPathLengthDif = Math.abs(dbPathLength - nodesToCheck.get(osmNodeId)[0]);
                            osmPathLength = nodesToCheck.get(osmNodeId)[0];
                            countOfDists1 = nodesToCheck.get(osmNodeId)[3];
                            osmPath = (ArrayList<Long>) osmNeighbourNet.get(osmNodeId);
                            osmPath.add(osmNodeId);
                        }
                    }
                    else if (nodesToCheck.get(osmNodeId)[1] + nodesToCheck.get(osmNodeId)[2] > viceOsmPath[1].get(0)) {
                        viceOsmPathLength = nodesToCheck.get(osmNodeId)[0];
                        countOfDists2 = nodesToCheck.get(osmNodeId)[3];
                        viceOsmPath[0] = (ArrayList<Long>) osmNeighbourNet.get(osmNodeId);
                        viceOsmPath[0].add(osmNodeId);
                        viceOsmPath[1].set(0, (long) (nodesToCheck.get(osmNodeId)[1] + nodesToCheck.get(osmNodeId)[2]));
                    }
                    continue;
                }
                List<Long> osmPathSoFar = new ArrayList<>(osmNeighbourNet.get(osmNodeId));
                osmPathSoFar.add(osmNodeId);
                for (Long nId : osmNodes.get(osmNodeId).neighbours) {
                    osmNeighbourNet.put(nId, osmPathSoFar);

                    double osmPathLengthSoFar = nodesToCheck.get(osmNodeId)[0] + getDistance(osmNodes.get(osmNodeId).lat, osmNodes.get(osmNodeId).lon, osmNodes.get(nId).lat, osmNodes.get(nId).lon) * 1000.0;
                    updatedNodesToCheck.put(nId,
                            new Double[]{osmPathLengthSoFar,
                                    osmNodes.get(nId).tags.containsValue("switch") ? nodesToCheck.get(osmNodeId)[1]+1 : nodesToCheck.get(osmNodeId)[1],
                                    osmNodes.get(nId).tags.containsValue("railway_crossing") ? nodesToCheck.get(osmNodeId)[2]+1 : nodesToCheck.get(osmNodeId)[2],
                                    nodesToCheck.get(osmNodeId)[3]+1});
                }
            }
        }
        ArrayList<DBNode> showDBPath = new ArrayList<>();
        ArrayList<OSMNode> showOSMPath = new ArrayList<>();
        for (Long dbNodeShow : dbPath)
            showDBPath.add(dbNodes.get(dbNodeShow));
        for (Long osmNodeShow : osmPath)
            showOSMPath.add(osmNodes.get(osmNodeShow));

        if (!found1) {
            osmPath = viceOsmPath[0];
            osmPathLength = viceOsmPathLength;
        }
        else if (osmPath.size() != countOfDists1+1) {
            countOfMissedPaths2++;
            return;
        }
        if (osmPath.isEmpty()) {
            countOfMissedPaths1++;
            return;
        }

        //map nodes in paths
        OSMNode nodeBefore = null;
        OSMNode nodeAfter = null;
        double dbNodeDist = 0;
        double osmNodeDist = 0;

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
                if (Objects.equals(nextDbNode.type, "simple_switch") || Objects.equals(nextDbNode.type, "cross")) {
                    mapBestOsmNodeFit(nextDbNode, nodeBefore, osmPath);
                    break;
                }
                continue;
            }

            dbNodeDist += lastDbDist;
            double lastOsmDist;
            if (osmPath.size() == 2) {
                setGpsTag(nextDbNode, osmStartNode, osmEndNode, dbNodeDist);
                continue;
            }

            for (int j = breakIndex; j < osmPath.size()-1; j++) {
                OSMNode osmNode = osmNodes.get(osmPath.get(j));
                OSMNode nextOsmNode = osmNodes.get(osmPath.get(j + 1));
                //update osmNodeDist
                lastOsmDist = getDistance(osmNode.lat, osmNode.lon, nextOsmNode.lat, nextOsmNode.lon) * 1000.0;
                osmNodeDist += lastOsmDist;
                if (dbNodeDist/dbPathLength <= osmNodeDist/osmPathLength) {
                    nodeBefore = osmNode;
                    nodeAfter = nextOsmNode;
                    breakIndex = j+1;
                    break;
                }
            }
            //if switch or cross -> add to anchorNodes
            //breaks DB-Path-loop
            if (Objects.equals(nextDbNode.type, "simple_switch") || Objects.equals(nextDbNode.type, "cross")) {
                mapBestOsmNodeFit(nextDbNode, nodeBefore, osmPath);
                break;
            }
            //get gps-tag for dbNode
            setGpsTag(nextDbNode, nodeAfter, nodeBefore, osmNodeDist - (dbNodeDist/dbPathLength * osmPathLength));
        }
    }

    private static void mapBestOsmNodeFit(DBNode dbNode, OSMNode osmNode, ArrayList<Long> osmPath) {
        long bestOsmNodeFit = 0L;
        int j = 1;
        int k = 1;
        for (int i = osmPath.indexOf(osmNode.osmId); true; i += (j = (int) Math.pow(-1, k+1) * k), k++) {
            if ((i < 1 && i + j * -k > osmPath.size()-2) || (i + j * -k < 1 && i > osmPath.size()-2))
                break;
            if (i < 1 || i > osmPath.size()-2)
                continue;
            Long currentOsmNode = osmNodes.get(osmPath.get(i)).osmId;
            if ((Objects.equals(dbNode.type, "simple_switch") && osmNodes.get(currentOsmNode).tags.containsValue("switch"))
                    || (Objects.equals(dbNode.type, "cross") && osmNodes.get(currentOsmNode).tags.containsValue("railway_crossing"))) {
                if (osmNodes.get(currentOsmNode).tags.containsKey("ref")) {
                    if (Objects.equals(osmNodes.get(currentOsmNode).tags.get("ref"), dbNode.name1) || Objects.equals(osmNodes.get(currentOsmNode).tags.get("ref"), dbNode.name2)) {
                        bestOsmNodeFit = osmNodes.get(currentOsmNode).osmId;
                        break;
                    }
                    continue;
                }
                if (k < 6)
                    bestOsmNodeFit = osmNodes.get(currentOsmNode).osmId;
            }
        }
        if (bestOsmNodeFit == 0)
            return;
        if (anchorNodes.get(dbNode.nodeId) == null) {
            openAnchorNodes.put(dbNode.nodeId, bestOsmNodeFit);
            anchorNodes.put(dbNode.nodeId, bestOsmNodeFit);
        }
        dbNode.lat = osmNodes.get(bestOsmNodeFit).lat;
        dbNode.lon = osmNodes.get(bestOsmNodeFit).lon;
        dbNodes.put(dbNode.nodeId, dbNode);
    }

    private static void setGpsTag(DBNode dbNode, OSMNode nodeOne, OSMNode nodeTwo, double dist) {
        dbNode.lat = nodeOne.lat + (nodeTwo.lat - nodeOne.lat) * (dist/1000.0);
        dbNode.lon = nodeOne.lon + (nodeTwo.lon - nodeOne.lon) * (dist/1000.0);
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
    public static void dumpMappedNodes() {
        for (Integer outputWay : dbWays.keySet()) {
            File file = new File("data/mappedDBNodes/" + outputWay + ".csv");
            try {
                FileWriter writer = new FileWriter(file);
                writer.write("section_id;element_id;km;type;ds100;station_name;name1;name2;lat;lon\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (DBNode outputNode : dbNodes.values()) {
            try {
                FileWriter writer = new FileWriter("data/mappedDBNodes/" + outputNode.streckenId + ".csv", true);
                writer.write(outputNode.print()+"\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}