/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.net.SocketAddress;

public class ContactNode extends Node{
    
    protected SocketAddress address;
    private int failures = 0;
    
    public ContactNode(KUID nodeId, SocketAddress address) {
        super(nodeId);
        this.address = address;
    }
    
    public int failure() {
        return ++failures;
    }
    
    public int getFailureCount() {
        return failures;
    }
    
    public void updateTimeStamp() {
        super.updateTimeStamp();
        failures = 0;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public SocketAddress setSocketAddress(SocketAddress address) {
        SocketAddress o = this.address;
        this.address = address;
        return o;
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
