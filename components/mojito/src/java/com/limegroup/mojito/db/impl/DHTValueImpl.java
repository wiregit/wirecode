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

package com.limegroup.mojito.db.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueType;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;

/**
 * An implementation of DHTValue
 */
public class DHTValueImpl implements DHTValue, Serializable {
    
    private static final long serialVersionUID = -5172585009004184680L;

    /** The originator of the DHTValue */
    private Contact creator;
    
    /** The sender who sent the DHTValue to us (store forward) */
    private Contact sender;
    
    /** The Key of the DHTValue */
    private KUID valueId;
    
    /** The Value type */
    private DHTValueType type;
    
    /** The version of this DHT value */
    private int version;
    
    /** The Value */
    private byte[] data;
    
    /** The time when this DHTValue object was created */
    private long creationTime = System.currentTimeMillis();
    
    /** The time when this DHTValue was republished */
    private transient long lastRepublishingTime = 0L;
    
    /** The number of locations where this value was stored */
    private transient int locationCount = 0;
    
    /** Whether or not this DHTValue is a local value */
    private boolean isLocalValue = true;
    
    /** The hashCode */
    private int hashCode;
    
    
    /**
     * Creates an instance of DHTValueImpl
     * 
     * @param creator The originator of the DHTValue
     * @param sender The sender of the DHTValue
     * @param valueId The KUID of the DHTValue
     * @param type The type of the DHTValue
     * @param version The version of the DHTValue
     * @param data The actual DHTValue
     * @param isLocalValue Whether or not it's a local DHTValue
     */
    public DHTValueImpl(Contact creator, Contact sender, 
            KUID valueId, DHTValueType type, int version, byte[] data, boolean isLocalValue) {
        
        if (type == null) {
            type = DHTValueType.BINARY;
        }
        
        if (data == null || data.length == 0) {
            this.data = EMPTY_DATA;
        }
        
        this.creator = creator;
        this.sender = sender;
        this.valueId = valueId;
        this.type = type;
        this.version = version;
        this.data = data;
        
        this.isLocalValue = isLocalValue;
        
        // Compute the HashCode
        hashCode = 17 * valueId.hashCode() + Arrays.hashCode(data);
    }
    
    /**
     * Initializes the DHTValue
     */
    private void init() {
        lastRepublishingTime = 0L;
        locationCount = 0;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getValueID()
     */
    public KUID getValueID() {
        return valueId;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getValueType()
     */
    public DHTValueType getValueType() {
        return type;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getData()
     */
    public byte[] getData() {
        return data;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#size()
     */
    public int size() {
        return data.length;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#isEmpty()
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /** 
     * Sets the creator, meant for internal use only! 
     */
    public void setCreator(Contact creator) {
        this.creator = creator;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getOriginator()
     */
    public Contact getCreator() {
        return creator;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getOriginatorID()
     */
    public KUID getCreatorID() {
        return creator.getNodeID();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getSender()
     */
    public Contact getSender() {
        return sender;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getCreationTime()
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getPublishTime()
     */
    public long getPublishTime() {
        return lastRepublishingTime;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#isDirect()
     */
    public boolean isDirect() {
        return creator.getNodeID().equals(sender.getNodeID());
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#isLocalValue()
     */
    public boolean isLocalValue() {
        return isLocalValue;
    }
    
    public int getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#isRepublishingRequired()
     */
    public boolean isRepublishingRequired() {
        if (!isLocalValue()) {
            return false;
        }
        
        long t = (long)((locationCount 
                * DatabaseSettings.VALUE_REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // Do never republish more than every X minutes
        long nextPublishTime = Math.max(t, 
                DatabaseSettings.MIN_VALUE_REPUBLISH_INTERVAL.getValue());
        
        long time = lastRepublishingTime + nextPublishTime;
        return System.currentTimeMillis() >= time;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#setLocationCount(int)
     */
    public void setLocationCount(int locationCount) {
        if (locationCount < 0) {
            throw new IllegalArgumentException("locations: " + locationCount);
        }
        
        this.locationCount = locationCount;
        this.lastRepublishingTime = System.currentTimeMillis();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValue#getLocationCount()
     */
    public int getLocationCount() {
        return locationCount;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValueImpl)) {
            return false;
        }
        
        DHTValueImpl c = (DHTValueImpl)o;
        return valueId.equals(c.valueId) && Arrays.equals(data, c.data);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (isEmpty()) {
            buffer.append(valueId).append(", creator=")
                .append(getCreator()).append(" (REMOVE)");
        } else {
            buffer.append(valueId).append("\n")
                .append("Type: ").append(getValueType()).append("\n")
                .append("Creator: ").append(getCreator()).append("\n")
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
