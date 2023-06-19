package main.java;

public class Fixpoint {
    public Fixpoint(long myOSMNodeId, long osmId, long myDBNodeId, long sectionId, long elementId) {
        this.myOSMNodeId = myOSMNodeId;
        this.osmId = osmId;
        this.myDBNodeId = myDBNodeId;
        this.sectionId = sectionId;
        this.elementId = elementId;
    }
    public long myOSMNodeId;
    public long osmId;
    public long myDBNodeId;
    public long sectionId;
    public long elementId;
}
