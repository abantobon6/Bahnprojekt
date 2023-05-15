package main.java;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException, FileNotFoundException {

        InputStream input = new FileInputStream(new File("data/germany_railway.osm"));

        OsmIterator iterator = new OsmXmlIterator(input,false);

        for(EntityContainer container : iterator) {
              if (container.getType() == EntityType.Relation) {
                  System.out.println("Id: " + container.getEntity().getId());
                  System.out.println("Tag: " + container.getEntity().getTag(0));
                  System.out.println();
              }
        }
    }
}