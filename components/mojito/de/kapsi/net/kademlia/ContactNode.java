/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.net.SocketAddress;

public class ContactNode extends Node {
    
    private static final long serialVersionUID = -5416538917308950549L;

    protected SocketAddress address;
    
    private int failures = 0;
    
    private long firstAliveTime = 0L;
    
    public ContactNode(KUID nodeId, SocketAddress address) {
        super(nodeId);
        this.address = address;
    }
    
    public int failure() {
        return ++failures;
    }
    
    public boolean hasFailed() {
        return (failures > 0);
    }
    
    public int getFailureCount() {
        return failures;
    }
    
    public void alive() {
        super.alive();
        failures = 0;
        if(firstAliveTime == 0L) {
            firstAliveTime = System.currentTimeMillis();
        }
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public SocketAddress setSocketAddress(SocketAddress address) {
        SocketAddress o = this.address;
        this.address = address;
        return o;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ContactNode)) {
            return false;
        }
        
        ContactNode other = (ContactNode)o;
        return nodeId.equals(other.nodeId) 
                    && address.equals(other.address);
    }
    
    public String toString() {
        return nodeId + " (" + address + ")" + ", failures: "+failures;
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
