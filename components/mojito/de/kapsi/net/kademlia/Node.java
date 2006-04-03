package de.kapsi.net.kademlia;

import java.io.Serializable;

public abstract class Node implements Serializable {
    
    protected final KUID nodeId;
    
    private long timeStamp = 0L;
    
    public Node(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    public void alive() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(long timestamp) {
        this.timeStamp = timestamp;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Node)) {
            return false;
        }
        
        return nodeId.equals(((Node)o).nodeId);
    }
    
    public String toString() {
        return nodeId.toString();
    }
}
