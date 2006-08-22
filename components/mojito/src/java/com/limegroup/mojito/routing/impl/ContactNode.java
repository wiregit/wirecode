/*
 * Mojito Distributed Hash Table (Mojito DHT)
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * An implementation of Contact
 */
public class ContactNode implements Contact {
    
    private static final long serialVersionUID = 833079992601013124L;

    private static final Log LOG = LogFactory.getLog(Contact.class);
    
    private KUID nodeId;
    
    private int vendor;
    
    private int version;
    
    private int instanceId;
    
    private transient SocketAddress sourceAddress;
    
    private SocketAddress contactAddress;
    
    private transient long rtt = -1L;
    
    private long timeStamp = 0L;
    
    private long lastFailedTime = 0L;
    
    private int failures = 0;
    
    private transient State state = State.UNKNOWN;
    
    private boolean firewalled = false;
    
    /**
     * 
     * @param sourceAddress
     * @param vendor
     * @param version
     * @param nodeId
     * @param contactAddress
     * @param instanceId
     * @param firewalled
     * @param state
     */
    public ContactNode(SocketAddress sourceAddress, int vendor, int version, 
            KUID nodeId, SocketAddress contactAddress, 
            int instanceId, boolean firewalled, State state) {
        
        if (nodeId == null) {
            throw new NullPointerException("Node ID is null");
        }
        
        if (contactAddress == null) {
            throw new NullPointerException("SocketAddress is null");
        }
        
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version must be between 0x00 and 0xFFFF: " + version);
        }
        
        this.sourceAddress = sourceAddress;
        this.nodeId = nodeId;
        this.contactAddress = contactAddress;
        this.instanceId = instanceId;
        this.firewalled = firewalled;
        this.state = (state != null ? state : State.UNKNOWN);
        
        if (state == State.ALIVE) {
            this.timeStamp = System.currentTimeMillis();
            
            fixSourceAndContactAddress(sourceAddress);
        }
    }
    
    private void init() {
        sourceAddress = null;
        rtt = -1;
        state = State.UNKNOWN;
    }

    /**
     * This method takes the InetAddress from the sourceAddress and 
     * the Port number from the contactAddress and combines them
     * to the new contactAddress. When the Port number is 0 it will
     * set the sourceAddress as contactAddress and marks this Contact
     * as firewalled.
     */
    public void fixSourceAndContactAddress(SocketAddress sourceAddress) {
        if (sourceAddress != null) {
            this.sourceAddress = sourceAddress;
            
            int port = ((InetSocketAddress)contactAddress).getPort();
            if (port == 0) {
                if (!firewalled && LOG.isErrorEnabled()) {
                    LOG.error(ContactUtils.toString(nodeId, sourceAddress) 
                            + " contact address is set to Port 0 but it is not marked as firewalled");
                }
                
                contactAddress = sourceAddress;
                firewalled = true;
            } else {
                contactAddress = new InetSocketAddress(
                        ((InetSocketAddress)sourceAddress).getAddress(), port);
            }
        }
    }
    
    public void updateWithExistingContact(Contact existing) {
        if (!nodeId.equals(existing.getNodeID())) {
            throw new IllegalArgumentException("Node IDs do not match: " + this + " vs. " + existing);
        }
        
        if (rtt < 0L) {
            rtt = existing.getRoundTripTime();
        }
        
        if (!isAlive() || (getTimeStamp() < existing.getTimeStamp())) {
            timeStamp = existing.getTimeStamp();
            lastFailedTime = existing.getLastFailedTime();
            failures = existing.getFailures();
        }
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
    
    public SocketAddress getContactAddress() {
        return contactAddress;
    }
    
    public SocketAddress getSourceAddress() {
        return sourceAddress;
    }
    
    public long getRoundTripTime() {
        return rtt;
    }
    
    public void setRoundTripTime(long rtt) {
        this.rtt = rtt;
    }
    
    public void setTimeStamp(long timeStamp) {
        assert (timeStamp != LOCAL_CONTACT);
        this.timeStamp = timeStamp;
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public long getLastFailedTime() {
        return lastFailedTime;
    }

    public boolean isFirewalled() {
        return firewalled;
    }

    public long getAdaptativeTimeout() {
        //for now, based on failures and previous round trip time
        long timeout = NetworkSettings.TIMEOUT.getValue();
        if(rtt <= 0L || !isAlive()) {
            return timeout;
        } else {
            return Math.min(timeout, 
                ((NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.getValue() * rtt) + failures * rtt));
        }
    }
    
    public void alive() {
        state = State.ALIVE;
        failures = 0;
        timeStamp = System.currentTimeMillis();
    }
    
    public boolean isAlive() {
        return state == State.ALIVE;
    }
    
    public void unknown() {
        state = State.UNKNOWN;
        failures = 0;
        timeStamp = 0L;
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
    
    public int getFailures() {
        return failures;
    }
    
    public void handleFailure() {
        failures++;
        lastFailedTime = System.currentTimeMillis();
        
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
    
    /**
     * Returns the State of this Contact
     */
    public State getState() {
        return state;
    }
    
    /**
     * Sets the State of this Contact
     */
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
                && contactAddress.equals(c.getContactAddress());
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ContactUtils.toString(getNodeID(), getContactAddress()))
            .append(", rtt=").append(getRoundTripTime())
            .append(", failures=").append(getFailures())
            .append(", instanceId=").append(getInstanceID())
            .append(", state=").append(getState())
            .append(", firewalled=").append(isFirewalled());
        
        return buffer.toString();
    }
}
