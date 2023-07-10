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
        mapNodes();
        writeNodesToFile();
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
                System.out.println(dbWays.get(streckenId).streckenId);
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
                //System.out.println("Progress: Node " + ctr);
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
                    OSMNode nodeToUpdate = osmNodes.get(node.osmId);
                    nodeToUpdate.ways.add(newOSMWay);
                    osmNodes.put(node.osmId, nodeToUpdate);
                }
                osmWays.put(tmpWay.getId(), newOSMWay);
                //System.out.println("Progress: Way " + (ctr - osmNodes.size()));
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

                            //System.out.println("OSMNode: " + fix.osmNode.toString());
                            //System.out.println("DBNode: " + fix.dbNode.toString());
                            //System.out.println();

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
        for (DBWay dbWay : dbWays.values()) {
            for (DBSection dbSection : dbWay.dbSections.values()) {
                for (DBNode dbNode : dbSection.nodes) {
                    List<DBNode> dbNodesToScan = dbSection.nodes;
                    Fixpoint fix;
                    if ((fix = getFixpoint(dbNode)) != null) {
                        long kmUp = dbNode.km;
                        long kmDown = dbNode.km;
                        int indexUp = dbNodesToScan.indexOf(dbNode);
                        int indexDown = dbNodesToScan.indexOf(dbNode);
                        DBNode currentDBNodeUp = dbNode;
                        DBNode currentDBNodeDown = dbNode;
                        List<DBNode> dbWayUp = new ArrayList<>();
                        List<DBNode> dbWayDown = new ArrayList<>();


                        OSMNode startOSMNode = fix.osmNode;
                        List<List<OSMNode>> osmPaths = new ArrayList<>();
                        List<OSMNode> firstPath = new LinkedList<>();
                        firstPath.add(startOSMNode);
                        osmPaths.add(firstPath);


                        //Ways entlang gehen
                        while (true) {
                            if (indexUp < dbNodesToScan.size() - 1 || indexDown > 0) {
                                if (indexUp < dbNodesToScan.size() - 1) {
                                    //DBWay aufwährts gehen
                                    indexUp++;
                                    dbWayUp.add(dbNodesToScan.get(indexUp));
                                }
                                if (indexDown > 0) {
                                    //DBWay abwährts gehen
                                    indexDown--;
                                    dbWayDown.add(dbNodesToScan.get(indexDown));
                                }
                                //OSMWays in alle Richtungen entlanglaufen
                                osmPaths = expandOSMPaths(osmPaths);

                                //update data for next iteration
                                currentDBNodeUp = dbNodesToScan.get(indexUp);
                                currentDBNodeDown = dbNodesToScan.get(indexDown);
                                kmUp = currentDBNodeUp.km;
                                kmDown = currentDBNodeDown.km;

                                if (getFixpoint(currentDBNodeUp) != null || getFixpoint(currentDBNodeDown) != null) {
                                    for (List<OSMNode> osmPath : osmPaths) {
                                        for (OSMNode neighbour : osmPath.get(osmPath.size() - 1).getNeigbours()) {
                                            if (Objects.equals(getFixpoint(currentDBNodeUp), getFixpoint(neighbour))) {

                                            }
                                            else if (Objects.equals(getFixpoint(currentDBNodeDown), getFixpoint(neighbour))) {

                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static List<List<OSMNode>> expandOSMPaths(List<List<OSMNode>> osmPaths) {
        List<List<OSMNode>> workOSMPaths = osmPaths;
        int indexOfLastElement = workOSMPaths.size() - 1;
        for (List<OSMNode> path : workOSMPaths) {
            List<OSMNode> workPath = path;
            List<OSMNode> workPath2 = path;
            List<OSMNode> workPath3 = path;
            if (path.get(indexOfLastElement).getNeigbours().size() > 1) {
                workPath.add(workPath.get(indexOfLastElement).getNeigboursWithout(workPath.get(indexOfLastElement)).get(0));
                workOSMPaths.remove(path);
                workOSMPaths.add(workPath);

                if (path.get(indexOfLastElement).getNeigbours().size() > 2) {
                    workPath2.add(workPath2.get(indexOfLastElement).getNeigboursWithout(workPath2.get(indexOfLastElement)).get(1));
                    workOSMPaths.add(workPath2);

                    if (path.get(indexOfLastElement).getNeigbours().size() > 3) {
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

    private static void writeNodesToFile() {

    }

}