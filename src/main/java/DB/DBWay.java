package main.java.DB;

import java.util.List;

public class DBWay {
    public DBWay(long streckenId, List<DBSection> dbSectionIds) {
        this.streckenId = streckenId;
        this.dbSectionIds = dbSectionIds;
    }

    public long streckenId;
    public List<DBSection> dbSectionIds;
}
