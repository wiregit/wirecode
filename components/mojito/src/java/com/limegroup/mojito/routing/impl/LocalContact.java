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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The LocalContact stores the Contact information of the local Node. 
 * Some Methods of LocalContact do nothing or return hard coded 
 * values like getting and setting the Round Trip Time (RTT) and 
 * LocalContact provides some additional Methods to update the
 * local Node's Contact informations.
 */
public class LocalContact implements Contact {

    private static final Log LOG = LogFactory.getLog(LocalContact.class);
    
    private static final long serialVersionUID = -1372388406248015059L;

    private volatile int vendor;
    
    private volatile int version;
    
    private volatile KUID nodeId;
    
    private volatile int instanceId;
    
    private volatile boolean firewalled;
    
    private transient volatile SocketAddress sourceAddress;
    
    private transient volatile SocketAddress contactAddress;
    
    private transient SocketAddress tmpExternalAddress;
    
    public LocalContact(int vendor, int version, KUID nodeId, 
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
    
    /**
     * Sets the local Node's vendor code
     */
    public void setVendor(int vendor) {
        this.vendor = vendor;
    }
    
    public int getVendor() {
        return vendor;
    }
    
    /**
     * Sets the local Node's version number
     */
    public void setVersion(int version) {
        this.version = version;
    }
    
    public int getVersion() {
        return version;
    }
    
    /**
     * Sets the local Node's KUID
     * 
     * NOTE: This requires a rebuild of the RouteTable!
     */
    public void setNodeID(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public int getInstanceID() {
        return instanceId;
    }
    
    /**
     * Sets the instanceId to the next value
     */
    public void nextInstanceID() {
        instanceId = (instanceId + 1) % 0xFF;
    }

    public SocketAddress getContactAddress() {
        return contactAddress;
    }
    
    /**
     * Sets the local Node's Contact (external) Address
     */
    public synchronized void setContactAddress(SocketAddress contactAddress) {
        this.contactAddress = contactAddress;
        this.tmpExternalAddress = null;
    }
    
    /**
     * Sets the local Node's Contact (external) Port
     */
    public synchronized void setExternalPort(int port) {
        InetSocketAddress addr = (InetSocketAddress)getContactAddress();
        setContactAddress(new InetSocketAddress(addr.getAddress(), port));
    }
    
    /**
     * Returns the local Node's external Port
     */
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
            if (LOG.isErrorEnabled()) {
                LOG.error("Argument is null");
            }
            return;
        }
        
        // --- DOES NOT CHANGE THE PORT! ---
        
        InetAddress externalAddress = ((InetSocketAddress)externalSocketAddress).getAddress();
        //int externalPort = ((InetSocketAddress)externalSocketAddress).getPort();
        
        InetAddress currentAddress = ((InetSocketAddress)getContactAddress()).getAddress();
        int currentPort = ((InetSocketAddress)getContactAddress()).getPort();
        
        if (externalAddress.equals(currentAddress)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Reported external address is equal to current external address: " + externalAddress);
            }
            return;
        }
        
        // There's no reason to set the external address to a
        // PRIVATE IP address. This can happen with a Node that
        // is pinging an another Node that is behind the same
        // NAT router
        if (NetworkUtils.isPrivateAddress(externalAddress)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(externalAddress + " is a PRIVATE address");
            }
            return;
        }
        
        if (!NetworkUtils.isSameAddressSpace(
                        externalAddress, currentAddress)) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("The current external address " + currentAddress 
                        + " is from a different IP address space than " + externalAddress);
            }
            return;
        }
        
        SocketAddress addr = new InetSocketAddress(externalAddress, currentPort);
        
        if (tmpExternalAddress == null 
                || tmpExternalAddress.equals(addr)) {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Setting the current external address from " 
                        + contactAddress + " to " + addr);
            }
            
            contactAddress = addr;
            
            //if (externalPort == currentPort) {}
        }
        
        tmpExternalAddress = addr;
    }
    
    /**
     * Hard coded to return 0
     */
    public int getFailures() {
        return 0;
    }
    
    /**
     * Hard coded to return 0L
     */
    public long getLastFailedTime() {
        return 0L;
    }
    
    /**
     * Does nothing
     */
    public void setRoundTripTime(long rtt) {
    }
    
    /**
     * Hard coded to return 0L
     */
    public long getRoundTripTime() {
        return 0L;
    }

    /**
     * Does nothing
     */
    public void setTimeStamp(long timeStamp) {
    }
    
    /** 
     * Hard coded to return @see #LOCAL_CONTACT
     */
    public long getTimeStamp() {
        return LOCAL_CONTACT;
    }
    
    /**
     * Hard coded to return 0L
     */
    public long getAdaptativeTimeout() {
        return 0L;
    }
    
    /**
     * Does nothing
     */
    public void handleFailure() {
    }

    /**
     * Hard coded to return true
     */
    public boolean hasBeenRecentlyAlive() {
        return true;
    }

    /**
     * Hard coded to return false
     */
    public boolean hasFailed() {
        return false;
    }

    /**
     * Hard coded to return false. This might sound
     * strange since the local Node is always alive 
     * but the idea in some cases is to contact alive
     * Nodes only and as there's no reason to contact
     * the local Node.
     */
    public boolean isAlive() {
        return false;
    }

    /**
     * Hard coded to return false
     */
    public boolean isDead() {
        return false;
    }

    /**
     * Hard coded to return true.
     * 
     * @see #isAlive()
     */
    public boolean isUnknown() {
        return true;
    }

    /**
     * Does nothing
     */
    public void unknown() {
    }
    
    /**
     * Does nothing if 'existing' is the same instance 
     * as 'this'. Throws an UnsupportedOperationException
     * if 'existing' is a different instance.
     * 
     * The UnsupportedOperationException exception is mainly
     * thrown for debugging purposes and should never ever
     * happen. If it does then there's a bug in the RouteTable
     * update logic!
     */
    public void updateWithExistingContact(Contact existing) {
        if (existing != this) {
            throw new UnsupportedOperationException();
        }
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LocalContact)) {
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
