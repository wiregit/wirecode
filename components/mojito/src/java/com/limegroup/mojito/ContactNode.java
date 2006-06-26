/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito;

import java.net.SocketAddress;

import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.settings.RouteTableSettings;

/**
 * A ContactNode is an actual Node in DHT
 */
public class ContactNode extends Node implements Contact {
    
    private static final long serialVersionUID = -5416538917308950549L;

    private static final int FIREWALLED = 0x01;
    
    /** The Vendor ID of the Node */
    private int vendor;
    
    /** The Version of the Node */
    private int version;
    
    /** The IPP of the Node */
    private SocketAddress address;
    
    /** The instance ID of the Node */
    private int instanceId;

    /** Various flags (like FIREWALLED) this Node has set */
    private int flags;
    
    /** The number of failures (no response for requests) */
    private int failures = 0;
    
    private long lastDeadOrAliveTime = 0L;
    
    /** The RTT of the Node */
    private transient long roundTripTime = -1L;
    
    public ContactNode(int vendor, int version, 
            KUID nodeId, SocketAddress address) {
        this(vendor, version, nodeId, address, 0, 0);
    }
    
    public ContactNode(int vendor, int version, 
            KUID nodeId, SocketAddress address, 
            int instanceId, int flags) {
        super(nodeId);
        
        if (address == null) {
            throw new NullPointerException("SocketAddress is null");
        }
        
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version must be between 0x00 and 0xFFFF: " + version);
        }
        
        this.vendor = vendor;
        this.version = version;
        this.address = address;
        this.instanceId = instanceId;
        this.flags = flags;
    }
    
    public int getVendor() {
        return vendor;
    }
    
    public int getVersion() {
        return version;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public SocketAddress setSocketAddress(SocketAddress address) {
        SocketAddress o = this.address;
        this.address = address;
        return o;
    }
    
    public int getInstanceID() {
        return instanceId;
    }
    
    public void setInstanceID(int instanceId) {
        this.instanceId = instanceId;
    }

    public int getFlags() {
        return flags;
    }
    
    public long getAdaptativeTimeout() {
        //for now, based on failures and previous round trip time
        long maxTimeout = NetworkSettings.MAX_TIMEOUT.getValue();
        if(roundTripTime <= 0L || isDead()) {
            return maxTimeout;
        } else {
            return Math.min(maxTimeout, 
                ((NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.getValue() * roundTripTime) + failures * roundTripTime));
        }
    }
    
    public long getRoundTripTime() {
        return roundTripTime;
    }

    public void setRoundTripTime(long rountTripTime) {
        this.roundTripTime = rountTripTime;
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
    
    public void unknownState() {
        failures = 0;
        setTimeStamp(0L);
        lastDeadOrAliveTime = 0L;
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
    
    public boolean equals(Object o) {
        if (!(o instanceof ContactNode)) {
            return false;
        }
        
        return super.equals(o) 
                && address.equals(((ContactNode)o).address);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(toString(getNodeID(), getSocketAddress()))
            .append(", failures: ").append(failures)
            .append(", instanceId: ").append(instanceId)
            .append(", unknown: ").append(getTimeStamp()==0L);
        
        if (isFirewalled()) {
            buffer.append(", firewalled: ").append(isFirewalled());
        }
        
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
