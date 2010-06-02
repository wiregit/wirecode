/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.routing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
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
    
    /** This Contact's Node ID. */
    private final KUID nodeId;
    
    /** The Vendor code of this Contact. */
    private final Vendor vendor;
    
    /** The Version of this Contact. */
    private final Version version;
    
    /** The instance ID of this Contact. */
    private final int instanceId;
    
    /** The IP:Port of the Contact as reported in the IP Packet. */
    private transient volatile SocketAddress sourceAddress;
    
    /** The IP:Port of the Contact as reported in the DHTMessage. */
    private volatile SocketAddress contactAddress;
    
    /** The Round Trip Time (RTT). */
    private transient long rtt = -1L;
    
    /** The time of the last successful contact. */
    private volatile long timeStamp = 0L;
    
    /** The time of the last unsuccessful contact.  */
    private volatile long lastFailedTime = 0L;
    
    /** The number of errors that have occurred. */
    private volatile int failures = 0;
    
    /** The current state of the Node. */
    private transient volatile State state = State.UNKNOWN;
    
    /** Whether or not this Node is firewalled. */
    private volatile int flags = DEFAULT_FLAG;
    
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
        
        if (State.ALIVE.equals(state) ||
                (State.UNKNOWN.equals(state) && sourceAddress != null)) {
            this.timeStamp = System.currentTimeMillis();
            
            fixSourceAndContactAddress(sourceAddress);
        }
        
        checkPortConsistent();
    }
    
    private void checkPortConsistent() {
        int port = ((InetSocketAddress)contactAddress).getPort();
        if (port == 0)
            setFirewalled(true);
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
    public final void fixSourceAndContactAddress(SocketAddress sourceAddress) {
        if (sourceAddress != null) {
            this.sourceAddress = sourceAddress;
            
            SocketAddress backup = contactAddress;
            
            int port = NetworkUtils.getPort(contactAddress);
            if (port == 0) {
                if (!isFirewalled()) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(ContactUtils.toString(nodeId, sourceAddress) 
                                + " contact address is set to Port 0 but it is not marked as firewalled");
                    }
                }
                
                contactAddress = sourceAddress;
                checkPortConsistent();
                
            } else if (!NetworkSettings.ACCEPT_FORCED_ADDRESS.getValue()) {
                // If the source address is a PRIVATE address then 
                // don't use it because the other Node is on the 
                // same LAN as we are (damn NAT routers!).
                if (!ContactUtils.isPrivateAddress(sourceAddress)) {
                    contactAddress = NetworkUtils.merge(sourceAddress, port);
                }
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Merged " + sourceAddress + " and " 
                    + backup + " to " + contactAddress + ", firewalled=" + isFirewalled());
            }
        }
    }
    
    public void updateWithExistingContact(Contact existing) {
        if (!nodeId.equals(existing.getContactId())) {
            throw new IllegalArgumentException("Node IDs do not match: " + this + " vs. " + existing);
        }
        
        if (rtt < 0L) {
            rtt = existing.getRoundTripTimeInMillis();
        }
        
        if (!isAlive() || (getTimeStamp() < existing.getTimeStamp())) {
            timeStamp = existing.getTimeStamp();
            lastFailedTime = existing.getLastFailedTime();
            failures = existing.getFailures();
        }
    }
    
    @Override
    public Vendor getVendor() {
        return vendor;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public KUID getContactId() {
        return nodeId;
    }
    
    @Override
    public int getInstanceId() {
        return instanceId;
    }
    
    @Override
    public int getFlags() {
        return flags;
    }
    
    @Override
    public SocketAddress getContactAddress() {
        return contactAddress;
    }
    
    @Override
    public SocketAddress getSourceAddress() {
        return sourceAddress;
    }
    
    @Override
    public long getRoundTripTime(TimeUnit unit) {
        return unit.convert(rtt, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public long getRoundTripTimeInMillis() {
        return getRoundTripTime(TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void setRoundTripTime(long rtt, TimeUnit unit) {
        this.rtt = unit.toMillis(rtt);
    }
    
    @Override
    public void setTimeStamp(long timeStamp) {
        assert (timeStamp != LOCAL_CONTACT);
        this.timeStamp = timeStamp;
    }
    
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }
    
    @Override
    public long getLastFailedTime() {
        return lastFailedTime;
    }

    @Override
    public boolean isFirewalled() {
        return (flags & FIREWALLED_FLAG) != 0;
    }
    
    private void setFirewalled(boolean firewalled) {
        if (isFirewalled() != firewalled) {
            this.flags ^= FIREWALLED_FLAG;
        }
    }
    
    @Override
    public long getAdaptativeTimeout(long defaultValue, TimeUnit unit) {
        //for now, based on failures and previous round trip time
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
        
        if (0 < rtt && isAlive()) {
            // Should be NetworkSettings.MIN_TIMEOUT_RTT < t < NetworkSettings.DEFAULT_TIMEOUT
            long rttFactor = NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.getValue();
            long adaptiveTimeout = ((rttFactor * rtt) + failures * rtt);
            timeout = Math.max(Math.min(timeout, adaptiveTimeout), 
                    NetworkSettings.MIN_TIMEOUT_RTT.getTimeInMillis());
        }
        
        return unit.convert(timeout, TimeUnit.MILLISECONDS);
    }
    
    public void alive() {
        state = State.ALIVE;
        failures = 0;
        timeStamp = System.currentTimeMillis();
    }
    
    @Override
    public boolean isAlive() {
        return State.ALIVE.equals(state);
    }
    
    @Override
    public void unknown() {
        state = State.UNKNOWN;
        failures = 0;
        timeStamp = 0L;
    }
    
    @Override
    public boolean isUnknown() {
        return State.UNKNOWN.equals(state);
    }
    
    @Override
    public boolean isDead() {
        return State.DEAD.equals(state);
    }
    
    @Override
    public boolean hasBeenRecentlyAlive() {
        long time = System.currentTimeMillis() - getTimeStamp();
        return time < RouteTableSettings.MIN_RECONNECTION_TIME.getTimeInMillis();
    }
    
    @Override
    public int getFailures() {
        return failures;
    }
    
    @Override
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
    
    @Override
    public boolean hasFailed() {
        return failures > 0;
    }
    
    /**
     * Returns the state of this Contact.
     */
    public State getState() {
        return state;
    }
    
    /**
     * Sets the state of this Contact.
     */
    public void setState(State state) {
        if (state == null) {
            state = State.UNKNOWN;
        }
        this.state = state;
    }
    
    @Override
    public boolean isShutdown() {
        return (flags & SHUTDOWN_FLAG) != 0;
    }
    
    @Override
    public void shutdown(boolean shutdown) {
        if (isShutdown() != shutdown) {
            this.flags ^= SHUTDOWN_FLAG;
            this.state = State.DEAD;
        }
    }
    
    @Override
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Contact) 
                || o instanceof LocalContact) {
            return false;
        }
        
        Contact c = (Contact)o;
        return nodeId.equals(c.getContactId())
                && contactAddress.equals(c.getContactAddress());
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ContactUtils.toString(getContactId(), getContactAddress()))
            .append(", rtt=").append(getRoundTripTimeInMillis())
            .append(", failures=").append(getFailures())
            .append(", instanceId=").append(getInstanceId())
            .append(", state=").append(isShutdown() ? "DOWN" : getState())
            .append(", firewalled=").append(isFirewalled());
        
        return buffer.toString();
    }
}
