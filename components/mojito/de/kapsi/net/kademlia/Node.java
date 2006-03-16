package de.kapsi.net.kademlia;

public abstract class Node {
    
    protected final KUID nodeId;
    
    protected long timeStamp = 0L;
    
    public Node(KUID nodeId) {
        this.nodeId = nodeId;
        alive();
    }
    
    public void alive() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
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
