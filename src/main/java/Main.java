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
    private static HashSet<Fixpoint> fixpoints = new HashSet<>();
    private static HashSet<OSMNode> resultNodes = new HashSet<>();

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
        System.out.println("Start mapping nodes...");
        mapNodes();
        System.out.println("Finished mapping nodes...");
        System.out.println("Mapped " + resultNodes.size() + " nodes.");
        System.out.println("Start writing nodes to file...");
        try {
            writeNodesToFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished!");
    }

    private static void readDB() {
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("data/DB"));
            for (Path filePath : ds) {
                int streckenId = Integer.parseInt(filePath.toString().substring(8, 12));

                HashMap<Long, DBSection> sections = new HashMap<>();

                BufferedReader br = null;
                String line = "";
                br = new BufferedReader(new FileReader(filePath.toFile()));
                long ctr = 0;
                br.readLine();
                while((line = br.readLine()) != null) {
                    String[] tags = line.split(";", -1);
                    DBNode node = new DBNode(streckenId, ctr, Long.parseLong(tags[0]), Long.parseLong(tags[1]), Long.parseLong(tags[2]), tags[3], tags[4], tags[5], tags[6], tags[7]);
                    dbNodes.put(Long.parseLong(tags[1]), node);

                    sections.putIfAbsent(Long.parseLong(tags[2]), new DBSection(Long.parseLong(tags[2]), new ArrayList<>()));
                    List<DBNode> updatedNodeList = sections.get(Long.parseLong(tags[2])).nodes;
                    updatedNodeList.add(node);
                    sections.put(Long.parseLong(tags[2]), new DBSection(Long.parseLong(tags[2]), updatedNodeList));

                    ctr++;
                }
                dbWays.put(streckenId, new DBWay(streckenId, sections));
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
                osmNodes.put(tmpNode.getId(), new OSMNode(convertListToMap(tmpNode.getTags()), tmpNode.getId(), ctr, tmpNode.getLongitude(), tmpNode.getLatitude()));
            }

            if (container.getType() == EntityType.Way) {
                tmpWay = (Way) container.getEntity();
                List<OSMNode> nodes = new ArrayList<>();
                for (long nodeId_way : tmpWay.getNodes().toArray()) {
                    //füge passende Nodes zu Liste hinzu
                    nodes.add(osmNodes.get(nodeId_way));
                }
                //füge Ways hinzu
                OSMWay newOSMWay = new OSMWay(tmpWay.getNodes(), nodes, tmpWay.getId(), ctr, convertListToMap(tmpWay.getTags()));
                for (OSMNode node : newOSMWay.nodes) {
                    if (node == null)
                        continue;
                    OSMNode nodeToUpdate = osmNodes.get(node.osmId);
                    nodeToUpdate.ways.add(newOSMWay);
                    osmNodes.put(node.osmId, nodeToUpdate);
                }
                osmWays.put(tmpWay.getId(), newOSMWay);
            }
            ctr++;
        }
    }

    private static Map<String, String> convertListToMap(List<? extends OsmTag> list) {
        Map<String, String> map = new HashMap<>();
        for (OsmTag tag : list) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    private static void findFixpoints() {
        List<OSMNode> osmSwitches = new ArrayList<>();
        List<DBNode> dbSwitches = new ArrayList<>();

        for (OSMNode osmNode : osmNodes.values()) {
            if (osmNode.tags.containsValue("switch") && osmNode.tags.containsKey("ref"))
                osmSwitches.add(osmNode);
        }

        for (DBNode dbNode : dbNodes.values()) {
            if(Objects.equals(dbNode.type, "simple_switch"))
                dbSwitches.add(dbNode);
        }

        for (OSMWay way : osmWays.values()) {
            if(!way.tags.containsKey("ref"))
                continue;

            String wayRef = way.tags.get("ref");
            for (OSMNode osmNode : way.nodes) {
                if (osmSwitches.contains(osmNode)) {
                    for (DBNode dbSwitch : dbSwitches) {
                        if (Objects.equals(String.valueOf(dbSwitch.streckenId), wayRef) &&
                                (Objects.equals(dbSwitch.name1, osmNode.tags.get("ref")))) {
                            Fixpoint fix = new Fixpoint(osmNode, getDBSwitch(dbSwitches, wayRef, osmNode.tags.get("ref")));

                            fixpoints.add(fix);
                        }
                    }
                }
            }
        }
    }

    private static DBNode getDBSwitch(List<DBNode> dbSwitches, String wayRef, String ref) {
        for (DBNode dbSwitch : dbSwitches) {
            if (Objects.equals(String.valueOf(dbSwitch.streckenId), wayRef) && Objects.equals(dbSwitch.name1, ref))
                return dbSwitch;
        }
        return null;
    }

    private static void mapNodes() {
        int counter = 0;
        for (DBWay dbWay : dbWays.values()) {
            System.out.println(counter + "/" + dbWays.values().size());
            counter++;
            for (DBSection dbSection : dbWay.dbSections.values()) {
                for (DBNode dbNode : dbSection.nodes) {

                    System.out.println("StreckenId: " + dbWay.streckenId + " | SectionId: " + dbNode.sectionId + " | NodeId: " +  dbNode.elementId);

                    List<DBNode> dbNodesToScan = dbSection.nodes; //section (nodes with consistent kilometre-marking)
                    Fixpoint fix = getFixpoint(dbNode);
                    if (fix != null) { //find first Fixpoint
                        //set variables and direction
                        long currentKm = dbNode.km;
                        int firstIndex = dbSection.nodes.indexOf(dbNode);
                        int currentIndex = firstIndex;
                        List<DBNode> currentDBPath = new ArrayList<>();

                        //set datastructure for osm-paths
                        OSMNode startOSMNode = fix.osmNode;
                        ArrayList<ArrayList<OSMNode>> osmPaths = new ArrayList<>();
                        ArrayList<OSMNode> firstPath = new ArrayList<>();
                        firstPath.add(startOSMNode);
                        osmPaths.add(firstPath);

                        //walk DBSection
                        while (currentIndex < dbNodesToScan.size() && currentIndex >= 0) {
                            System.out.println("B");

                            //walk DBWay up
                            if (firstIndex == 0) {
                                currentDBPath.add(dbNodesToScan.get(currentIndex));
                                currentIndex++;
                            }
                            //walk DBWay down
                            else if (firstIndex == dbNodesToScan.size()-1) {
                                currentDBPath.add(dbNodesToScan.get(currentIndex));
                                currentIndex--;
                            }
                            if (currentIndex >= dbNodesToScan.size() || currentIndex < 0|| getFixpoint(dbNodesToScan.get(currentIndex)) != null)
                                break;
                        }
                        //walk OSMWays (in all directions)
                        boolean cont = true;
                        while (cont && osmPaths.size() < 1050) {
                            osmPaths = expandOSMPaths(osmPaths);
                            for (List<OSMNode> osmPath : osmPaths) {
                                if (Objects.equals(fix, getFixpoint(osmPath.get(osmPath.size() - 1)))) {
                                    System.out.println("A");
                                    processWays(currentDBPath, osmPath);
                                    cont = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Finished " + dbWay.streckenId);
        }
    }

    private static void processWays(List<DBNode> dbPath, List<OSMNode> osmPath) {
        if (dbPath.size() == 0 || osmPath.size() == 0)
            return;
        int currentOSMIndex = 0;
        int currentDBIndex = 0;
        OSMNode currentOSMNode = osmPath.get(0);
        DBNode currentDBNode = dbPath.get(0);
        long currentKM = currentDBNode.km;

        currentOSMNode.tags.put("dbElementID", String.valueOf(dbPath.get(currentDBIndex + 1).elementId));
        resultNodes.add(currentOSMNode);

        while (true) {
            double osmDist = getDistance(currentOSMNode.lat, currentOSMNode.lon, osmPath.get(currentOSMIndex + 1).lat, osmPath.get(currentOSMIndex + 1).lon);
            double dbDist = Math.abs(currentKM - dbPath.get(currentDBIndex + 1).km);
            //Next node is present in both lists
            if (Math.abs(osmDist - dbDist) < 0.010) {
                OSMNode mappedNode = osmPath.get(currentOSMIndex + 1);
                mappedNode.tags.put("dbElementID", String.valueOf(dbPath.get(currentDBIndex + 1).elementId));
                resultNodes.add(mappedNode);
                fixpoints.add(new Fixpoint(currentOSMNode, currentDBNode));

                currentDBIndex++;
                currentOSMIndex++;
                currentDBNode = dbPath.get(currentDBIndex);
                currentOSMNode = osmPath.get(currentOSMIndex);
                currentKM = currentDBNode.km;
            }
            //Next node is only present in OSM-list
            else if (osmDist + 0.010 < dbDist) {
                resultNodes.add(currentOSMNode);

                currentOSMIndex++;
                currentOSMNode = osmPath.get(currentOSMIndex);
                currentKM = currentKM + (long) osmDist;
            }
            //Next node is only present in DB-list
            else {
                Map<String, String> tags = new HashMap<>();
                tags.put("railway", String.valueOf(dbPath.get(currentDBIndex + 1).type));
                tags.put("dbElementID", String.valueOf(dbPath.get(currentDBIndex + 1).elementId));
                //compute lon and lat of new result-node
                double Bx = Math.cos(osmPath.get(currentOSMIndex+1).lat) * Math.cos(Math.abs(currentDBNode.km-currentKM));
                double By = Math.cos(osmPath.get(currentOSMIndex+1).lat) * Math.sin(Math.abs(currentDBNode.km-currentKM));
                double lat3 = Math.atan2(Math.sin(currentOSMNode.lat)+Math.sin(osmPath.get(currentOSMIndex+1).lat), Math.sqrt((Math.cos(currentOSMNode.lat)+Bx) * (Math.cos(currentOSMNode.lat)+Bx) + By*By));
                double lon3 = currentOSMNode.lon + Math.atan2(By, Math.cos(currentOSMNode.lat) + Bx);

                OSMNode newNode = new OSMNode(tags, -1, -1, lon3, lat3);
                resultNodes.add(newNode);

                currentKM = dbPath.get(currentDBIndex + 1).km;
                currentOSMNode = newNode;
                currentDBIndex++;
                currentDBNode = dbPath.get(currentDBIndex);
            }
            if (currentDBIndex == dbPath.size() - 1)
                break;
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

    private static ArrayList<ArrayList<OSMNode>> expandOSMPaths(ArrayList<ArrayList<OSMNode>> osmPaths) {
        ArrayList<ArrayList<OSMNode>> workOSMPaths = (ArrayList<ArrayList<OSMNode>>)osmPaths.clone();
        int counter = 0;
        for (ArrayList<OSMNode> path : osmPaths) {
            System.out.println("Path " + counter +"/"+ osmPaths.size());
            counter++;
            int indexOfLastElement = path.size() - 1;
            ArrayList<OSMNode> workPath = path;
            ArrayList<OSMNode> workPath2 = path;
            ArrayList<OSMNode> workPath3 = path;
            int numberOfNeighbours = path.get(indexOfLastElement).getNeigbours().size();
            if (numberOfNeighbours > 1) {
                System.out.println("Number of neighbours: " + numberOfNeighbours + "|" +
                        path.get(indexOfLastElement).getNeigboursWithout(path.get(indexOfLastElement)).size());
                workPath.add(workPath.get(indexOfLastElement).getNeigboursWithout(workPath.get(indexOfLastElement)).get(0));
                workOSMPaths.remove(path);
                workOSMPaths.add(workPath);

                if (numberOfNeighbours > 2) {
                    for (OSMNode node : path.get(indexOfLastElement).getNeigbours()) {
                        System.out.println(node.osmId);
                    }
                    workPath2.add(workPath2.
                            get(indexOfLastElement).
                            getNeigboursWithout(workPath2.get(indexOfLastElement)).
                            get(1));
                    workOSMPaths.add(workPath2);

                    if (numberOfNeighbours > 3) {
                        workPath2.add(workPath3.get(indexOfLastElement).getNeigboursWithout(workPath3.get(indexOfLastElement)).get(2));
                        workOSMPaths.add(workPath3);
                    }
                }
            }
        }
        return workOSMPaths;
    }

    private static Fixpoint getFixpoint(Object node) {
        for (Fixpoint fix : fixpoints) {
            if (Objects.equals(fix.dbNode, node))
                return fix;
            if (Objects.equals(fix.osmNode, node))
                return fix;
        }
        return null;
    }

    private static void writeNodesToFile() throws IOException {
        File resultFile = new File("mapped_nodes.osm");
        BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile, true));
        for (OSMNode node : resultNodes) {
            writer.write(node.toString());
        }
    }

}