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
 
package org.limewire.mojito.routing.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.ContactUtils;


/**
 * The RemoteContact class implements the Contact interface 
 * and encapsulates all requires info of a remote Node.
 */
public class RemoteContact implements Contact {
    
    private static final long serialVersionUID = 833079992601013124L;

    private static final Log LOG = LogFactory.getLog(RemoteContact.class);
    
    /** This Contact's Node ID */
    private KUID nodeId;
    
    /** The Vendor code of this Contact */
    private Vendor vendor;
    
    /** The Version of this Contact */
    private Version version;
    
    /** The instance ID of this Contact */
    private int instanceId;
    
    /** The IP:Port of the Contact as reported in the IP Packet */
    private transient SocketAddress sourceAddress;
    
    /** The IP:Port of the Contact as reported in the DHTMessage */
    private SocketAddress contactAddress;
    
    /** The Rount Trip Time (RTT) */
    private transient long rtt = -1L;
    
    /** The time of the last successful contact */
    private long timeStamp = 0L;
    
    /** The time of the last unseccessful contact  */
    private long lastFailedTime = 0L;
    
    /** The number of errors that have occured */
    private int failures = 0;
    
    /** The current state of the Node */
    private transient State state = State.UNKNOWN;
    
    /** Whether or not this Node is firewalled */
    private int flags = DEFAULT_FLAG;
    
    public RemoteContact(SocketAddress sourceAddress, Vendor vendor, Version version, 
            KUID nodeId, SocketAddress contactAddress, 
            int instanceId, int flags, State state) {
        
        if (nodeId == null) {
            throw new NullPointerException("Node ID is null");
        }
        
        if (contactAddress == null) {
            throw new NullPointerException("SocketAddress is null");
        }
        
        this.sourceAddress = sourceAddress;
        this.vendor = vendor;
        this.version = version;
        this.nodeId = nodeId;
        this.contactAddress = contactAddress;
        this.instanceId = instanceId;
        this.flags = flags;
        this.state = (state != null ? state : State.UNKNOWN);
        
        if (State.ALIVE.equals(state)) {
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
            
            SocketAddress backup = contactAddress;
            
            int port = ((InetSocketAddress)contactAddress).getPort();
            if (port == 0) {
                if (!isFirewalled()) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(ContactUtils.toString(nodeId, sourceAddress) 
                                + " contact address is set to Port 0 but it is not marked as firewalled");
                    }
                }
                
                contactAddress = sourceAddress;
                setFirewalled(true); // Force Firewalled!
                
            } else if (!NetworkSettings.ACCEPT_FORCED_ADDRESS.getValue()) {
                // If the source address is a PRIVATE address then 
                // don't use it because the other Node is on the 
                // same LAN as we are (damn NAT routers!).
                if (!NetworkUtils.isPrivateAddress(sourceAddress)) {
                    contactAddress = new InetSocketAddress(
                            ((InetSocketAddress)sourceAddress).getAddress(), port);
                }
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Merged " + sourceAddress + " and " 
                    + backup + " to " + contactAddress + ", firewalled=" + isFirewalled());
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
    
    public Vendor getVendor() {
        return vendor;
    }

    public Version getVersion() {
        return version;
    }

    public KUID getNodeID() {
        return nodeId;
    }
    
    public int getInstanceID() {
        return instanceId;
    }
    
    public int getFlags() {
        return flags;
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
        return (flags & FIREWALLED_FLAG) != 0;
    }
    
    private void setFirewalled(boolean firewalled) {
        if (isFirewalled() != firewalled) {
            this.flags ^= FIREWALLED_FLAG;
        }
    }
    
    public long getAdaptativeTimeout() {
        //for now, based on failures and previous round trip time
        long timeout = NetworkSettings.TIMEOUT.getValue();
        if(rtt <= 0L || !isAlive()) {
            return timeout;
        } else {
            //should be NetworkSettings.MIN_TIMEOUT_RTT < t < NetworkSettings.TIMEOUT
            return Math.max(Math.min(timeout, 
                ((NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.getValue() * rtt) + failures * rtt)),
                NetworkSettings.MIN_TIMEOUT_RTT.getValue());
        }
    }
    
    public void alive() {
        state = State.ALIVE;
        failures = 0;
        timeStamp = System.currentTimeMillis();
    }
    
    public boolean isAlive() {
        return State.ALIVE.equals(state);
    }
    
    public void unknown() {
        state = State.UNKNOWN;
        failures = 0;
        timeStamp = 0L;
    }
    
    public boolean isUnknown() {
        return State.UNKNOWN.equals(state);
    }
    
    public boolean isDead() {
        return State.DEAD.equals(state);
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
        
        if (!isShutdown()) {
            // Node has ever been alive?
            if (getTimeStamp() > 0L) {
                if (failures >= RouteTableSettings.MAX_ALIVE_NODE_FAILURES.getValue()) {
                    state = State.DEAD;
                }
            } else {
                if (failures >= RouteTableSettings.MAX_UNKNOWN_NODE_FAILURES.getValue()) {
                    state = State.DEAD;
                }
            }
        }
    }
    
    public boolean hasFailed() {
        return failures > 0;
    }
    
    /**
     * Returns the state of this Contact
     */
    public State getState() {
        return state;
    }
    
    /**
     * Sets the state of this Contact
     */
    public void setState(State state) {
        if (state == null) {
            state = State.UNKNOWN;
        }
        this.state = state;
    }
    
    public boolean isShutdown() {
        return (flags & SHUTDOWN_FLAG) != 0;
    }
    
    public void shutdown(boolean shutdown) {
        if (isShutdown() != shutdown) {
            this.flags ^= SHUTDOWN_FLAG;
            this.state = State.DEAD;
        }
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Contact) 
                || o instanceof LocalContact) {
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
            .append(", state=").append(isShutdown() ? "DOWN" : getState())
            .append(", firewalled=").append(isFirewalled());
        
        return buffer.toString();
    }
}
