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

package com.limegroup.mojito.routing.impl;

import java.io.Serializable;
import java.net.SocketAddress;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.settings.RouteTableSettings;

public class ContactNode implements Contact, Serializable {
    
    private static final long serialVersionUID = 833079992601013124L;

    private static final int FIREWALLED = 0x01;
    
    private int vendor;
    
    private int version;
    
    private KUID nodeId;
    
    private int instanceId;
    
    private SocketAddress address;
    
    private transient long rtt = -1L;
    
    private transient long timeStamp = 0L;
    
    private transient long lastDeadOrAliveTime = 0L;
    
    private transient int flags = 0;
    
    private transient int failures = 0;
    
    private transient State state = State.UNKNOWN;
    
    public ContactNode(int vendor, int version, KUID nodeId, 
            SocketAddress address, State state) {
        this(vendor, version, nodeId, address, 0, 0, state);
    }
    
    public ContactNode(int vendor, int version, 
            KUID nodeId, SocketAddress address, 
            int instanceId, int flags, State state) {
        
        if (nodeId == null) {
            throw new NullPointerException("Node ID is null");
        }
        
        if (address == null) {
            throw new NullPointerException("SocketAddress is null");
        }
        
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version must be between 0x00 and 0xFFFF: " + version);
        }
        
        this.nodeId = nodeId;
        this.address = address;
        this.instanceId = instanceId;
        this.flags = flags;
        
        this.state = (state != null ? state : State.UNKNOWN);
        
        if (state == State.ALIVE) {
            this.timeStamp = System.currentTimeMillis();
            this.lastDeadOrAliveTime = timeStamp;
        }
    }
    
    public void set(Contact node) {
        if (!nodeId.equals(node.getNodeID())) {
            throw new IllegalArgumentException("Node IDs do not match");
        }
        
        this.vendor = node.getVendor();
        this.version = node.getVersion();
        this.address = node.getSocketAddress();
        this.state = State.UNKNOWN;
        
        if (node.isAlive()) {
            this.state = State.ALIVE;
            
            this.instanceId = node.getInstanceID();
            
            if (node.getRoundTripTime() > 0L) {
                this.rtt = node.getRoundTripTime();
            }
            
            this.timeStamp = System.currentTimeMillis();
            this.lastDeadOrAliveTime = timeStamp;
            this.failures = 0;
        }
    }
    
    public int getFlags() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getVendor() {
        return vendor;
    }

    public int getVersion() {
        return version;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public int getInstanceID() {
        return instanceId;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public void setSocketAddress(SocketAddress address) {
        this.address = address;
    }
    
    
    public void setInstanceID(int instanceId) {
        this.instanceId = instanceId;
    }

    public long getRoundTripTime() {
        return rtt;
    }
    
    public void setRoundTripTime(long rtt) {
        this.rtt = rtt;
    }
    
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
        this.lastDeadOrAliveTime = timeStamp;
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public long getLastDeadOrAliveTime() {
        return lastDeadOrAliveTime;
    }

    public boolean isFirewalled() {
        return (flags & FIREWALLED) == FIREWALLED;
    }
    
    public void setFirewalled(boolean firewalled) {
        if (firewalled) {
            this.flags |= FIREWALLED;
        } else {
            this.flags &= ~FIREWALLED;
        }
    }
    
    public long getAdaptativeTimeout() {
        //for now, based on failures and previous round trip time
        long maxTimeout = NetworkSettings.MAX_TIMEOUT.getValue();
        if(rtt <= 0L || isDead()) {
            return maxTimeout;
        } else {
            return Math.min(maxTimeout, 
                ((NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.getValue() * rtt) + failures * rtt));
        }
    }
    
    public void alive() {
        state = State.ALIVE;
        failures = 0;
        timeStamp = System.currentTimeMillis();
        lastDeadOrAliveTime = timeStamp;
    }
    
    public boolean isAlive() {
        return state == State.ALIVE;
    }
    
    public void unknown() {
        state = State.UNKNOWN;
        failures = 0;
        timeStamp = 0L;
        lastDeadOrAliveTime = 0L;
    }
    
    public boolean isUnknown() {
        return state == State.UNKNOWN;
    }
    
    public boolean isDead() {
        return state == State.DEAD;
    }
    
    public boolean hasBeenRecentlyAlive() {
        return ((System.currentTimeMillis() - getTimeStamp())
                    < RouteTableSettings.MIN_RECONNECTION_TIME.getValue());
    }
    
    public int getFailureCount() {
        return failures;
    }
    
    public void handleFailure() {
        failures++;
        lastDeadOrAliveTime = System.currentTimeMillis();
        
        // Node has ever been alive?
        if (getTimeStamp() > 0L) {
            if (failures >= RouteTableSettings.MAX_LIVE_NODE_FAILURES.getValue()) {
                state = State.DEAD;
            }
        } else {
            if (failures >= RouteTableSettings.MAX_UNKNOWN_NODE_FAILURES.getValue()) {
                state = State.DEAD;
            }
        }
    }
    
    public boolean hasFailed() {
        return failures > 0;
    }
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        if (state == null) {
            state = State.UNKNOWN;
        }
        this.state = state;
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Contact)) {
            return false;
        }
        
        Contact c = (Contact)o;
        return nodeId.equals(c.getNodeID())
                && address.equals(c.getSocketAddress());
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(toString(getNodeID(), getSocketAddress()))
            .append(", failures=").append(getFailureCount())
            .append(", instanceId=").append(getInstanceID())
            .append(", state=").append(getState())
            .append(", firewalled=").append(isFirewalled());
        
        return buffer.toString();
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
