/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class ContactNode extends Node {
    
    private static final long serialVersionUID = -5416538917308950549L;

    private static final int FIREWALLED = 0x01;
    
    private SocketAddress address;
    private int flags;
    
    private int failures = 0;
    
    private long lastDeadOrAliveTime = 0L;
    
    private boolean isPinged;
    
    public ContactNode(KUID nodeId, SocketAddress address) {
        this(nodeId, address, 0);
    }
    
    public ContactNode(KUID nodeId, SocketAddress address, int flags) {
        super(nodeId);
        
        this.address = address;
        this.flags = flags;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public void setFirewalled(boolean firewalled) {
        if (firewalled) {
            this.flags |= FIREWALLED;
        } else {
            this.flags &= ~FIREWALLED;
        }
    }
    
    public boolean isFirewalled() {
        return (flags & FIREWALLED) == FIREWALLED;
    }
    
    public boolean failure() {
        failures++;
        lastDeadOrAliveTime = System.currentTimeMillis();
        return isDead();
    }
    
    public boolean hasFailed() {
        return (failures > 0);
    }
    
    public void setUnknown() {
        failures = 0;
        setTimeStamp(0L);
        lastDeadOrAliveTime = 0L;
    }
    
    public boolean isPinged() {
        return isPinged;
    }

    public void setPinged(boolean isPinged) {
        this.isPinged = isPinged;
    }

    public boolean isDead() {
        //node has ever been alive?
        if(getTimeStamp() > 0L) {
            if (failures >= RouteTableSettings.MAX_LIVE_NODE_FAILURES.getValue()) {
                return true;
            }
        } else {
            if (failures >= RouteTableSettings.MAX_UNKNOWN_NODE_FAILURES.getValue()) {
                return true;
            }
        }
        return false;
    }
    
    public long getLastDeadOrAliveTime() {
        return lastDeadOrAliveTime;
    }

    public int getFailureCount() {
        return failures;
    }
    
    public void alive() {
        super.alive();
        failures = 0;
        lastDeadOrAliveTime = System.currentTimeMillis();
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
        
        return super.equals(o) 
                && address.equals(((ContactNode)o).address);
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(toString(getNodeID(), address))
            .append(", failures: ").append(failures)
            .append(", unknown: ").append(getTimeStamp()==0);
        return buffer.toString();
    }
    
    
    
    public void setTimeStamp(long timestamp) {
        super.setTimeStamp(timestamp);
        lastDeadOrAliveTime = timestamp;
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
