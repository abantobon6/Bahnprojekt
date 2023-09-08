package main.java.DB;

import java.util.List;

public class DBSection {
    public DBSection(long sectionId, List<Long> nodes) {
        this.sectionId = sectionId;
        this.nodes = nodes;
    }
    public long sectionId;
    public List<Long> nodes;
}
