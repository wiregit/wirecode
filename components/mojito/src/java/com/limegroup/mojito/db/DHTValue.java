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

package com.limegroup.mojito.db;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;

/**
 * The DHTValue class represents a <key, value> tuple that
 * is stored on the DHT. Besides the actual <key, value> tuple 
 * it's also storing the originator of the DHTValue as well as
 * the sender of the DHTValue.
 */
public class DHTValue implements Serializable {
    
    private static final long serialVersionUID = -5172585009004184680L;

    public static final byte[] EMPTY_DATA = new byte[0];

    /** The originator of the DHTValue */
    private Contact originator;
    
    /** The sender who sent the DHTValue to us (store forward) */
    private Contact sender;
    
    /** The Key of the DHTValue */
    private KUID valueId;
    
    /** The Value */
    private byte[] data;
    
    /** The time when this DHTValue object was created */
    private long creationTime = System.currentTimeMillis();
    
    /** The time when this DHTValue was republished */
    private transient long lastRepublishingTime = 0L;
    
    /** The number of locations where this value was stored */
    private transient int locations = 0;
    
    /** Whether or not this DHTValue is a local value */
    private boolean isLocalValue = true;
    
    /** The hashCode, lazy initialization */
    private volatile int hashCode = -1;
    
    /** Creates and returns a local DHTValue */
    public static DHTValue createLocalValue(Contact originator, 
            KUID valueId, byte[] data) {
        return new DHTValue(originator, originator, valueId, data, true);
    }
    
    /** Creates and returns a remote DHTValue */
    public static DHTValue createRemoteValue(Contact originator, Contact sender, 
            KUID valueId, byte[] data) {
        return new DHTValue(originator, sender, valueId, data, false);
    }
    
    private DHTValue(Contact originator, Contact sender, KUID valueId, byte[] data, 
            boolean isLocalValue) {
        this.originator = originator;
        this.sender = sender;
        this.valueId = valueId;
        this.data = (data != null) ? data : EMPTY_DATA;
        this.isLocalValue = isLocalValue;
    }
    
    /**
     * Initializes the DHTValue
     */
    private void init() {
        lastRepublishingTime = 0L;
        locations = 0;
    }
    
    /** 
     * Returns the ValueID
     */
    public KUID getValueID() {
        return valueId;
    }
    
    /** 
     * Returns the Value. Beware: The returned byte array is 
     * <b>NOT</b> a copy!
     */
    public byte[] getData() {
        return data;
    }
    
    /** 
     * Returns the size of the value 
     */
    public int size() {
        return data.length;
    }
    
    /** 
     * Returns whether or not the value is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /** 
     * Sets the originator, meant for internal use only! 
     */
    public void setOriginator(Contact originator) {
        this.originator = originator;
    }
    
    /** 
     * Returns the originator of the value 
     */
    public Contact getOriginator() {
        return originator;
    }
    
    /**
     * Returns the Node ID of the originator
     */
    public KUID getOriginatorID() {
        return originator.getNodeID();
    }
    
    /** 
     * Returns the sender of the value 
     */
    public Contact getSender() {
        return sender;
    }
    
    /** 
     * Returns the creationTime of this DHTValue object 
     */ 
    public long getCreationTime() {
        return creationTime;
    }
    
    /** 
     * Returns whether or not the originator and sender 
     * of the DHTValue are the same
     */
    public boolean isDirect() {
        return originator.getNodeID().equals(sender.getNodeID());
    }
    
    /** 
     * Returns whether or not this is a local DHTValue 
     */
    public boolean isLocalValue() {
        return isLocalValue;
    }
    
    /**
     * Returns true if this DHTValue requires republishing. Returns
     * always false if this is a non-local value.
     */
    public boolean isRepublishingRequired() {
        if (!isLocalValue()) {
            return false;
        }
        
        long t = (long)((locations 
                * DatabaseSettings.VALUE_REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // never republish more than every X minutes
        long nextPublishTime = Math.max(t, DatabaseSettings.MIN_VALUE_REPUBLISH_INTERVAL.getValue());
        long time = lastRepublishingTime + nextPublishTime;

        return System.currentTimeMillis() >= time;
    }
    
    /**
     * Sets the number of locations where this DHTValue was stored and 
     * the lastRepublishingTime to the current System time
     */
    public void publishedTo(int locations) {
        if (locations < 0) {
            throw new IllegalArgumentException("locations: " + locations);
        }
        
        this.locations = locations;
        this.lastRepublishingTime = System.currentTimeMillis();
    }
    
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = 17*valueId.hashCode() + Arrays.hashCode(data);
            if (hashCode == -1) {
                hashCode = 0;
            }
        }
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValue)) {
            return false;
        }
        
        DHTValue c = (DHTValue)o;
        return valueId.equals(c.valueId) && Arrays.equals(data, c.data);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (isEmpty()) {
            buffer.append(valueId).append(", originator=")
                .append(getOriginator()).append(" (REMOVE)");
        } else {
            buffer.append(valueId).append("\n")
                .append("Originator: ").append(getOriginator()).append("\n")
                .append("Sender: ").append(getSender()).append("\n")
                //.append("Hex: ").append(ArrayUtils.toHexString(data, 80));
                .append("Data: ").append(new String(data));
        }
        return buffer.toString();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(); // Init transient fields
    }
}
