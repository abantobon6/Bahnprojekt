package main.java;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;
import main.java.DB.DBNode;
import main.java.DB.DBPath;
import main.java.DB.DBSection;
import main.java.DB.DBWay;
import main.java.OSM.OSMNode;
import main.java.OSM.OSMPath;
import main.java.OSM.OSMWay;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    //registered nodes from filtered OpenStreetMap-data
    private static HashMap<Long, OSMNode> osmNodes = new HashMap<>();
    //registered nodes from DB-track-plan
    private static HashMap<Long,DBNode> dbNodes = new HashMap<>();
    //registered ways from OpenStreetMap-data
    private static HashMap<Long,OSMWay> osmWays = new HashMap<>();
    //registered tracks from DB-track-plan
    private static HashMap<Integer, DBWay> dbWays = new HashMap<>();
    //DBNodes mapped to element_id (f.e. switches only show up once)
    private static HashMap<Long, List<Long>> equalNodes = new HashMap<>();
    //ds100 mapped to its geographic middle
    private static HashMap<String, Double[]> betriebsstellen = new HashMap<String, Double[]>();
    //dbNodes mapped to osmNodes
    private static HashMap<Long, Long> anchorNodes = new HashMap<>();
    //anchorNodes with one or mor unmapped neighbours
    private static HashMap<Long, Long> openAnchorNodes = new HashMap<>();
    private static File mappedNodes = new File("data/mappedNodes.txt");
    private static FileWriter writer;

    static {
        try {
            writer = new FileWriter(mappedNodes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds gps coordinates to a number of DBNodes using the information from the OpenStreetMap data.
     * Creates a copy of the DB-data with added gps coordinates to the nodes.
     *
     * @param args
     */
    public static void main(String[] args) {
        start(1, false);
    }

    private static void start(int strategy, boolean rigorousExtension) {
        System.out.println("Starting OSM...");
        try {
            readOSM();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        mapDBNodes(strategy, rigorousExtension);
        System.out.println("Mapped " + dbNodes.values().stream().filter(x -> x.lat > 0).toList().size() + " nodes.");
        System.out.println("Start dumping...");
        dumpMappedNodes();
        System.out.println("Finished!");
    }
    /**
     * Calls the method representing the indicated strategy
     *
     * @param strategy indicates the mapping strategy
     */
    private static void mapDBNodes(int strategy, boolean rigorousExtension) {
        if (strategy == 1)
            mapNodesS1(rigorousExtension);
        if (strategy == 2)
            mapNodesS2(rigorousExtension);
    }

    /**
     * Prints some information regarding the registered DB-data.
     */
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

    /**
     * Prints some information regarding the registered OSM-data.
     */
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

    /**
     * Reads all the files in "data/DB" and stores the collected data in several HashMaps.
     */
    private static void readDB() {
        //read files
        try {
            //get nodes that are not connected to the main net
            List<Long> nodesToAvoid = new ArrayList<>();
            BufferedReader b = new BufferedReader(new FileReader("data/dbNodesToAvoid.txt"));
            String l;
            while ((l = b.readLine()) != null) {
                nodesToAvoid.add(Long.parseLong(l));
            }
            b.close();

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
                br.close();
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

    /**
     * Reads file "data/betriebsstelleToGPS.osm" and stores the collected data in HashMap betriebsstellen.
     */
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

    /**
     * Reads file "data/germany_railway.osm" and stores the collected data in several HashMaps.
     */
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

    /**
     * Searches for nodes that occur in both data sets and stores these found "anchor nodes" in anchorNodes.
     * Maps DB nodes to OSM nodes.
     */
    private static void findAnchorNodes() {
        List<DBNode> dbSwitches = equalNodes.values().stream().map(x -> dbNodes.get(x.get(0)))
                .filter(y -> (!y.name1.isEmpty() || !y.name2.isEmpty())
                        && (Objects.equals(y.type, "simple_switch")
                        || Objects.equals(y.type, "cross"))).toList();
        List<OSMNode> osmSwitches = osmNodes.values().stream()
                .filter(x -> x.tags.containsKey("ref")
                        && (x.tags.containsValue("switch")
                        || x.tags.containsValue("railway_crossing"))).toList();
        int ctr = 0;
        for (OSMNode osmSwitch : osmSwitches) {
            ctr++;
            //get nearest Betriebsstelle
            String nearestDS100 = "";
            double distToNearestBS = Double.MAX_VALUE;
            for (String bs : betriebsstellen.keySet()) {
                if (distToNearestBS > getDistance(osmSwitch.lat, osmSwitch.lon, betriebsstellen.get(bs)[0], betriebsstellen.get(bs)[1]) * 1000) {
                    distToNearestBS = getDistance(osmSwitch.lat, osmSwitch.lon, betriebsstellen.get(bs)[0], betriebsstellen.get(bs)[1]) * 1000;
                    nearestDS100 = bs;
                }
            }
            String finalNearestDS10 = nearestDS100;

            //get fitting DBNode
            for (DBNode dbSwitch : dbSwitches.stream().filter(z -> Objects.equals(z.ds100, finalNearestDS10)).toList()) {
                if ((Objects.equals(osmSwitch.tags.get("ref"), dbSwitch.name1)
                        || Objects.equals(osmSwitch.tags.get("ref"), dbSwitch.name2)
                        || Objects.equals(osmSwitch.tags.get("ref"), "W" + dbSwitch.name1)
                        || Objects.equals(osmSwitch.tags.get("ref"), "W" + dbSwitch.name2))) {
                    for (Long l : equalNodes.get(dbSwitch.elementId)) {
                        anchorNodes.put(l, osmSwitch.osmId);
                        openAnchorNodes.put(l, osmSwitch.osmId);
                        dbNodes.get(l).lat = osmSwitch.lat;
                        dbNodes.get(l).lon = osmSwitch.lon;
                    }
                }
            }
        }
    }

    /**
     * Takes a list containing OsmTags and converts it to a HashMap mapping the OsmTag-keys to their values.
     *
     * @param list List of OSMTags
     * @return Map of OsmTag-keys to OsmTag-values
     */
    private static Map<String, String> convertListToMap(List<? extends OsmTag> list) {
        Map<String, String> map = new HashMap<>();
        for (OsmTag tag : list) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    /**
     * Starts at a random unmapped Node and searches through the DB-Net until it finds two anchorNode.
     * Stores the path taken.
     */
    public static void mapNodesS2(boolean rigorousExtension) {
        LinkedList<Long> noGoodNodes = new LinkedList<>();
        DBNode middleNode;
        LinkedList<Long> startNodePath;
        LinkedList<Long> endNodePath;
        int ctr = 0;
        while (true) {
            List<Long> filteredDBNodes = dbNodes.keySet().stream().filter(x -> dbNodes.get(x).lat == -1 && !noGoodNodes.contains(x) && dbNodes.get(x).neighbours.size() > 1).toList();
            if (filteredDBNodes.isEmpty())
                break;
            middleNode = dbNodes.get(filteredDBNodes.get(new Random().nextInt(filteredDBNodes.size())));
            double dbPathLength = 0;
            int countOfBranches = 0;

            //find startNode and path
            DBPath path1 = getDBPathToAnchorNode(middleNode, dbNodes.get(middleNode.neighbours.get(0)), new HashSet<>());
            dbPathLength += path1.length;
            countOfBranches += path1.countOfBranches;
            startNodePath = path1.nodes;
            if (startNodePath.isEmpty()) {
                middleNode.lat = -2;
                middleNode.lon = -2;
                continue;
            }

            //find endNode and path
            DBPath path2 = getDBPathToAnchorNode(middleNode, dbNodes.get(middleNode.neighbours.get(1)), new HashSet<>(path1.nodes));
            dbPathLength += path2.length;
            countOfBranches += path2.countOfBranches;
            endNodePath = path2.nodes;
            if (endNodePath.isEmpty() || dbPathLength > 15000) {
                middleNode.lat = -2;
                middleNode.lon = -2;
                for (Long l : endNodePath) {
                    dbNodes.get(l).lat = -2;
                    dbNodes.get(l).lat = -2;
                }
                for (Long l : startNodePath) {
                    dbNodes.get(l).lat = -2;
                    dbNodes.get(l).lat = -2;
                }
                continue;
            }
            Collections.reverse(endNodePath);
            endNodePath.removeFirst();
            startNodePath.addAll(endNodePath);
            mapPath(dbNodes.get(startNodePath.getFirst()), dbNodes.get(startNodePath.getLast()), startNodePath, dbPathLength, middleNode, countOfBranches, rigorousExtension);
            try {
                writer.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (ctr == 500) {
                System.out.println("Mapped nodes: " + dbNodes.values().stream().filter(x -> x.lat > -1).toList().size());
                System.out.println("Remaining nodes to check: " + filteredDBNodes.size());
                ctr = 0;
            }
            ctr++;
        }
    }

    /**
     * Starts at a random anchorNode and searches through the DB-Net until it finds another anchorNode.
     * Stores the path taken.
     */
    public static void mapNodesS1(boolean rigorousExtension) {
        int ctr = 0;
        while (!openAnchorNodes.isEmpty()) {
            DBNode startNode = dbNodes.get(openAnchorNodes.keySet().stream().toList().get(new Random().nextInt(openAnchorNodes.size())));
            if (startNode.neighbours.stream().allMatch(x -> dbNodes.get(x).lat != -1)) {
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
                    if (anchorNodes.get(neighbourId) != null) {
                        //get dbPath
                        DBPath dbPath = getPath(dbNeighbourNet, neighbourId, startNode);
                        LinkedList<Long> path = dbPath.nodes;
                        double dbPathLength = dbPath.length;

                        Collections.reverse(path);
                        mapPath(startNode, dbNodes.get(neighbourId), path, dbPathLength, startNode, dbPath.countOfBranches, rigorousExtension);
                        try {
                            writer.write("\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (ctr == 500) {
                            System.out.println("Mapped nodes: " + dbNodes.values().stream().filter(x -> x.lat > -1).toList().size());
                            System.out.println("Size of OpenAnchorNodes" + ": " + openAnchorNodes.size());
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
            openAnchorNodes.remove(startNode.nodeId);
        }
    }

    /**
     * Gets the path indicated by neighbourId, startNode and dbNeighbourNet. The method
     * begins with neighbourId and ends when startNode ist reached.
     *
     * @param dbNeighbourNet HashMap that contains the path info. Every key-value-pair is a node pointing to the next one.
     * @param neighbourId Node to begin with
     * @param startNode Node to finish path at
     * @return the path
     */
    private static DBPath getPath(HashMap<Long, Long> dbNeighbourNet, Long neighbourId, DBNode startNode) {
        LinkedList<Long> dbPath = new LinkedList<>();
        double dbPathLength = 0;
        int countOfBranches = 0;

        if (Objects.equals(dbNodes.get(neighbourId).type, "simple_switch")
                || Objects.equals(dbNodes.get(neighbourId).type, "cross"))
            countOfBranches++;
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
            if (Objects.equals(dbNodes.get(dbNeighbourNet.get(tempId)).type, "simple_switch")
                    || Objects.equals(dbNodes.get(dbNeighbourNet.get(tempId)).type, "cross"))
                countOfBranches++;
            dbPath.add(dbNeighbourNet.get(tempId));
            tempId = dbNeighbourNet.get(tempId);
        }
        return new DBPath(dbPath, dbPathLength, countOfBranches);
    }

    /**
     * Computes a path through the DB railway net from middleNode, passing over to secondPathNode to any anchor node except nodeToAvoid
     *
     * @param middleNode beginning of the path
     * @param secondPathNode first step on the path
     * @param nodesToAvoid never a part of the path
     * @return a path from middleNode to any anchor node except nodeToAvoid
     */
    private static DBPath getDBPathToAnchorNode(DBNode middleNode, DBNode secondPathNode, HashSet<Long> nodesToAvoid) {
        HashMap<Long, Long> visitedNodes = new HashMap<>();
        visitedNodes.put(middleNode.elementId, 0L);
        HashMap<Long, Long> dbNeighbourNet = new HashMap<>();
        dbNeighbourNet.put(secondPathNode.nodeId, middleNode.nodeId);

        ArrayList<Long> updatedNodesToCheck = new ArrayList<>(List.of(secondPathNode.nodeId));
        ArrayList<Long> nodesToCheck;
        while (!updatedNodesToCheck.isEmpty()) {
            nodesToCheck = (ArrayList<Long>) updatedNodesToCheck.clone();
            updatedNodesToCheck = new ArrayList<>();
            for (Long l : nodesToCheck) {
                if (anchorNodes.get(l) != null && !nodesToAvoid.contains(l)) {
                    //get path from middleNode to found anchorNode

                    DBPath p = getPath(dbNeighbourNet, l, middleNode);
                    p.nodes.add(0, l);
                    return p;
                }
                visitedNodes.put(dbNodes.get(l).elementId, 0L);
                for (Long n : dbNodes.get(l).neighbours) {
                    if (visitedNodes.get(dbNodes.get(n).elementId) == null && !updatedNodesToCheck.contains(n)) {
                        dbNeighbourNet.put(n, l);
                        updatedNodesToCheck.add(n);
                    }
                }
            }
        }
        return new DBPath(new LinkedList<>(), 0, 0);
    }

    /**
     * Finds the best fitting (OSM-)path using OSM-data and maps the nodes within the DB-path.
     *
     * @param startNode start node of the path taken in mapNodes()
     * @param endNode end node of the path taken in mapNodes()
     * @param dbPath path taken in mapNodes()
     * @param dbPathLength length of the path taken in mapNodes()
     */
    private static void mapPath(DBNode startNode, DBNode endNode, List<Long> dbPath, double dbPathLength, DBNode middleNode, int countOfDbBranches, boolean rigorousExtension) {
        ArrayList<DBNode> showDBPath = new ArrayList<>();
        for (Long dbNodeShow : dbPath)
            showDBPath.add(dbNodes.get(dbNodeShow));


        //get related osmPath
        OSMNode osmStartNode = osmNodes.get(openAnchorNodes.get(startNode.nodeId));
        OSMNode osmEndNode = osmNodes.get(anchorNodes.get(endNode.nodeId));

        OSMPath osmPathObject = getOsmPathBetween(osmStartNode, osmEndNode, dbPath, dbPathLength, countOfDbBranches);
        double osmPathLength = osmPathObject.length;
        ArrayList<Long> osmPath = osmPathObject.nodes;

        ArrayList<OSMNode> showOSMPath = new ArrayList<>();
        for (Long osmNodeShow : osmPath)
            showOSMPath.add(osmNodes.get(osmNodeShow));

        if (osmPath.isEmpty()) {
            middleNode.lat = -2;
            middleNode.lon = -2;
            return;
        }

        try {
            writer.write("OSM-StartNode: " + osmStartNode + "\n");
            writer.write("OSM-EndNode: " + osmEndNode + "\n");
            writer.write("Laenge des OSM-Pfades: " + osmPathLength + "\n");
            for (Long l : osmPath)
                writer.write(osmNodes.get(l) + "\n");
            writer.write("DB-StartNode: " + startNode + "\n");
            writer.write("DB-EndNode: " + endNode + "\n");
            writer.write("Laenge des DB-Pfades: " + dbPathLength + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //map nodes in paths
        OSMNode nodeBefore;
        OSMNode nodeAfter;
        double dbNodeDist = 0;
        double osmNodeDist = 0;

        int breakIndex = 0;
        for (int i = 0; i < dbPath.size()-2; i++) {
            DBNode dbNode = dbNodes.get(dbPath.get(i));
            DBNode nextDbNode = dbNodes.get(dbPath.get(i+1));
            nodeBefore = osmNodes.get(osmPath.get(0));
            nodeAfter = osmNodes.get(osmPath.get(1));

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
                    if (nextDbNode.lat < 0)
                        if (mapBestOsmNodeFit(nextDbNode, nodeBefore, osmPath, rigorousExtension))
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
            if (nextDbNode.lat > 0)
                continue;
            //if switch or cross -> add to anchorNodes
            //breaks DB-Path-loop
            if (Objects.equals(nextDbNode.type, "simple_switch") || Objects.equals(nextDbNode.type, "cross")) {
                if (mapBestOsmNodeFit(nextDbNode, nodeBefore, osmPath, rigorousExtension))
                    break;
            }
            //get gps-tag for dbNode
            setGpsTag(nextDbNode, nodeAfter, nodeBefore, osmNodeDist - (dbNodeDist/dbPathLength * osmPathLength));
        }
    }

    /**
     * Finds a path through the OSM railway map from osmStartNode to osmEndNode with a length as close as possible to dbPathLength
     *
     * @param osmStartNode start node of the path to find
     * @param osmEndNode end node of the path to find
     * @param dbPathLength wanted length of the path to find
     * @return a path through the OSM railway map
     */
    private static OSMPath getOsmPathBetween(OSMNode osmStartNode, OSMNode osmEndNode, List<Long> dbPath, double dbPathLength, int countOfDbBranches) {
        OSMPath osmPath = new OSMPath(new ArrayList<>(), 0.0, 0);
        double bestPathLengthDif = Double.MAX_VALUE;

        ArrayList<OSMPath> newPotentialPaths = new ArrayList<>();
        ArrayList<OSMPath> potentialPaths;
        for (Long l : osmStartNode.neighbours) {
            int cob = 1;
            if (osmNodes.get(l).tags.containsValue("railway_crossing") || osmNodes.get(l).tags.containsValue("switch"))
                cob++;
            newPotentialPaths.add(new OSMPath(new ArrayList<>(List.of(osmStartNode.osmId, l)), getDistance(osmStartNode.lat, osmStartNode.lon, osmNodes.get(l).lat, osmNodes.get(l).lon) * 1000.0, cob));
        }

        while (!newPotentialPaths.isEmpty()) {
            potentialPaths = (ArrayList<OSMPath>) newPotentialPaths.clone();
            newPotentialPaths = new ArrayList<>();
            outerFor:
            for (OSMPath path : potentialPaths) {
                OSMNode lastNode = osmNodes.get(path.nodes.get(path.nodes.size() - 1));
                for (Long nId : lastNode.neighbours) {
                    if (lastNode.tags.containsValue("railway_crossing") || lastNode.tags.containsValue("switch"))
                        path.countOfBranches++;
                    if (lastNode.osmId == osmEndNode.osmId) {
                        if (Math.abs(path.length - dbPathLength) < bestPathLengthDif) {
                            bestPathLengthDif = Math.abs(path.length - dbPathLength);
                            osmPath = new OSMPath(new ArrayList<>(path.nodes), path.length, path.countOfBranches);
                        }
                        continue;
                    }
                    if (path.nodes.contains(nId) || Math.abs(path.length - dbPathLength) > bestPathLengthDif || path.countOfBranches > countOfDbBranches + 5 || path.length > dbPathLength * 1.1 + 50)
                        continue;
                    if (osmNodes.get(nId).tags.containsValue("switch") && osmNodes.get(nId).tags.containsKey("ref")
                            && dbPath.stream().noneMatch(x -> Objects.equals(dbNodes.get(x).name1, osmNodes.get(nId).tags.get("ref"))
                                                            || Objects.equals(dbNodes.get(x).name2, osmNodes.get(nId).tags.get("ref"))))
                        continue;
                    ArrayList<Long> nodes = new ArrayList<>(path.nodes);
                    nodes.add(nId);
                    newPotentialPaths.add(new OSMPath(nodes, path.length + getDistance(lastNode.lat, lastNode.lon, osmNodes.get(nId).lat, osmNodes.get(nId).lon) * 1000.0, 0 + path.countOfBranches));
                }
            }
        }
        return osmPath;
    }

    /**
     * Finds the nearest (OSM-)switch/cross with matching "ref" to the (DB-)switch/cross and storing it as an anchorNode.
     *
     * @param dbNode DB-switch/cross
     * @param osmNode OSM-switch/cross
     * @param osmPath OSM-path containing osmNode and fitting the DB-path containing dbNode
     */
    private static boolean mapBestOsmNodeFit(DBNode dbNode, OSMNode osmNode, ArrayList<Long> osmPath, boolean rigorousExtension) {
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
                        bestOsmNodeFit = currentOsmNode;
                        break;
                    }
                }
                //für rigoroses Erweitern der Ankerknoten
                else {
                    if (rigorousExtension)
                        bestOsmNodeFit = currentOsmNode;
                }
            }
        }
        if (bestOsmNodeFit == 0)
            return false;
        for (Long l : equalNodes.get(dbNode.elementId)) {
            if (anchorNodes.get(l) == null) {
                openAnchorNodes.put(l, bestOsmNodeFit);
                anchorNodes.put(l, bestOsmNodeFit);
            }
            dbNodes.get(l).lat = osmNodes.get(bestOsmNodeFit).lat;
            dbNodes.get(l).lon = osmNodes.get(bestOsmNodeFit).lon;
        }
        try {
            writer.write(dbNode + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Sets lan and lon of dbNode, using the gps tags from nodeOne and nodeTwo and the distance between dbNode and nodeOne.
     * dbNode has to lie on a straight line between nodeOne and nodeTwo.
     *
     * @param dbNode node to map
     * @param nodeOne start node of direct line
     * @param nodeTwo end node of direct line
     * @param dist distance from nodeOne to dbNode
     */
    private static void setGpsTag(DBNode dbNode, OSMNode nodeOne, OSMNode nodeTwo, double dist) {
        if (dbNode.lat == -1) {
            for (Long l : equalNodes.get(dbNode.elementId)) {
                dbNodes.get(l).lat = nodeOne.lat + (nodeTwo.lat - nodeOne.lat) * (dist / 1000.0);
                dbNodes.get(l).lon = nodeOne.lon + (nodeTwo.lon - nodeOne.lon) * (dist / 1000.0);
            }
        }
        try {
            writer.write(dbNode + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the distance between tho gps locations in km.
     *
     * @param lat1 latitude of first gps location
     * @param lon1 longitude of first gps location
     * @param lat2 latitude of second gps location
     * @param lon2 longitude of second gps location
     * @return distance between first and second gps location in km
     */
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        int radius = 6371;

        double lat = Math.toRadians(lat2 - lat1);
        double lon = Math.toRadians(lon2- lon1);

        double a = Math.sin(lat / 2) * Math.sin(lat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lon / 2) * Math.sin(lon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = radius * c;

        return Math.abs(d);
    }

    /**
     * Writes the DB-nodes in dbNodes, now containing gps location tags, in several csv-files (mirroring the input data files).
     */
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

        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("data/DB"));
            for (Path filePath : ds) {
                int streckenId = Integer.parseInt(filePath.toString().substring(8, 12));

                FileWriter writer = new FileWriter("data/mappedDBNodes/" + streckenId + ".csv", true);

                String line;
                BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()));
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    writer.write(dbNodes.get(Long.parseLong(tags[0] + tags[1])).print()+"\n");
                }
                writer.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}