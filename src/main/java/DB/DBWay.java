package main.java.DB;

import java.util.HashMap;
import java.util.List;

public class DBWay {
    public DBWay(long streckenId, HashMap<Long, DBSection> dbSections) {
        this.streckenId = streckenId;
        this.dbSections = dbSections;
    }

    public long streckenId;
    public HashMap<Long, DBSection> dbSections;
}
