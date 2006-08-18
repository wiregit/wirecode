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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.util.ContactUtils;

/**
 * A Contact for the local Node. 
 */
public class LocalContactImpl implements Contact {

    private static final long serialVersionUID = -1372388406248015059L;

    private volatile int vendor;
    
    private volatile int version;
    
    private volatile KUID nodeId;
    
    private volatile int instanceId;
    
    private volatile boolean firewalled;
    
    private transient volatile SocketAddress sourceAddress;
    
    private transient volatile SocketAddress contactAddress;
    
    private transient SocketAddress tmpExternalAddress;
    
    public LocalContactImpl(int vendor, int version, KUID nodeId, 
            int instanceId, boolean firewalled) {
        this.vendor = vendor;
        this.version = version;
        this.nodeId = nodeId;
        this.instanceId = instanceId;
        this.firewalled = firewalled;
        
        init();
    }
    
    private void init() {
        contactAddress = new InetSocketAddress("localhost", 0);
    }
    
    public int getVendor() {
        return vendor;
    }

    public int getVersion() {
        return version;
    }
    
    public void setNodeID(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public int getInstanceID() {
        return instanceId;
    }
    
    public void nextInstanceID() {
        instanceId = (instanceId + 1) % 0xFF;
    }

    public SocketAddress getContactAddress() {
        return contactAddress;
    }
    
    public synchronized void setContactAddress(SocketAddress contactAddress) {
        if (isFirewalled() && ((InetSocketAddress)contactAddress).getPort() != 0) {
            throw new IllegalStateException();
        }
        
        this.contactAddress = contactAddress;
        this.tmpExternalAddress = null;
    }
    
    public synchronized void setExternalPort(int port) {
        InetSocketAddress addr = (InetSocketAddress)getContactAddress();
        setContactAddress(new InetSocketAddress(addr.getAddress(), port));
    }
    
    public int getExternalPort() {
        return ((InetSocketAddress)getContactAddress()).getPort();
    }
    
    public SocketAddress getSourceAddress() {
        return sourceAddress;
    }
    
    public synchronized void setSourceAddress(SocketAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }
    
    public boolean isFirewalled() {
        return firewalled;
    }

    /**
     * Sets whether or not this Contact is firewalled
     */
    public void setFirewalled(boolean firewalled) {
        this.firewalled = firewalled;
    }
    
    /**
     * Sets the external Address of the local Node.
     */
    public synchronized void setExternalAddress(SocketAddress externalSocketAddress) {
        if (externalSocketAddress == null) {
            return;
        }
        
        // --- DOES NOT CHANGE THE PORT! ---
        
        InetAddress externalAddress = ((InetSocketAddress)externalSocketAddress).getAddress();
        //int externalPort = ((InetSocketAddress)externalSocketAddress).getPort();
        
        InetAddress currentAddress = ((InetSocketAddress)getContactAddress()).getAddress();
        int currentPort = ((InetSocketAddress)getContactAddress()).getPort();
        
        if (externalAddress.equals(currentAddress)) {
            return;
        }
        
        InetSocketAddress addr = new InetSocketAddress(externalAddress, currentPort);
        
        if (tmpExternalAddress == null 
                || tmpExternalAddress.equals(addr)) {
            setContactAddress(addr);
            
            //if (externalPort == currentPort) {}
        }
        
        tmpExternalAddress = addr;
    }
    
    public int getFailures() {
        return 0;
    }
    
    public long getLastFailedTime() {
        return 0L;
    }
    
    public void setRoundTripTime(long rtt) {
    }
    
    public long getRoundTripTime() {
        return 0L;
    }

    public void setTimeStamp(long timeStamp) {
    }
    
    public long getTimeStamp() {
        return Long.MAX_VALUE;
    }

    public long getAdaptativeTimeout() {
        return 0L;
    }
    
    public void handleFailure() {
    }

    public boolean hasBeenRecentlyAlive() {
        return true;
    }

    public boolean hasFailed() {
        return false;
    }

    public boolean isAlive() {
        return false;
    }

    public boolean isDead() {
        return false;
    }

    public boolean isUnknown() {
        return true;
    }

    public void unknown() {
    }
    
    public void updateWithExistingContact(Contact existing) {
        if (existing != this) {
            throw new UnsupportedOperationException();
        }
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Contact)) {
            return false;
        }
        
        return nodeId.equals(((Contact)o).getNodeID());
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) 
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ContactUtils.toString(getNodeID(), getContactAddress()))
            .append(", instanceId=").append(getInstanceID())
            .append(", firewalled=").append(isFirewalled());
        
        return buffer.toString();
    }
}
