/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.net.SocketAddress;

public class Node {
    
    protected final KUID nodeId;
    protected final SocketAddress address;
    
    private long timeStamp = 0L;
    private int failures = 0;
    
    public Node(KUID nodeId, SocketAddress address) {
        this.nodeId = nodeId;
        this.address = address;
        
        updateTimeStamp();
    }
    
    public int failure() {
        return ++failures;
    }
    
    public int getFailureCount() {
        return failures;
    }
    
    public void updateTimeStamp() {
        timeStamp = System.currentTimeMillis();
        failures = 0;
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
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
        return nodeId + " (" + address + ")";
    }
    
    public static String toString(KUID nodeId, SocketAddress address) {
        if (nodeId != null) {
            if (address != null) {
                return nodeId + " (" + address + ")";
            } else {
                return nodeId.toString();
            }
        } else if (address != null) {
            return address.toString();
        } else {
            return "null";
        }
    }
}
