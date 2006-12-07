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

import java.io.Serializable;
import java.util.Map;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.util.DatabaseUtils;

/**
 * A DHTValueEntity
 */
public class DHTValueEntity implements Map.Entry<KUID, DHTValue>, Serializable {
    
    private static final long serialVersionUID = 2007158043378144871L;

    /**
     * The creator of the value
     */
    private final Contact creator;
    
    /**
     * The sender of the value (store forward)
     */
    private final Contact sender;
    
    /**
     * The (primary) key of the value
     */
    private final KUID primaryKey;
    
    /**
     * The secondary key of the value
     */
    private final KUID secondaryKey;
    
    /**
     * The actual value
     */
    private final DHTValue value;
    
    /**
     * Whether or not this is a local value
     */
    private final boolean localValue;
    
    /**
     * The time when this value was created (local time)
     */
    private final long creationTime = System.currentTimeMillis();
    
    /**
     * If it's a non-local value we don't care about this
     * value as we're not republishing non-local values!
     * 
     * If it's a local value we assume it gets published
     * as soon as the value was created.
     */
    private transient long publishTime = 0L;
    
    /**
     * The number of locations where this value was stored
     */
    private transient int locationCount = 0;
    
    /**
     * The hash code of this entity
     */
    private final int hashCode;
    
    /**
     * 
     */
    public DHTValueEntity(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value, boolean localValue) {
        this(creator, sender, primaryKey, creator.getNodeID(), value, localValue);
    }
    
    /**
     * 
     */
    private DHTValueEntity(Contact creator, Contact sender, 
            KUID primaryKey, KUID secondaryKey, DHTValue value, boolean localValue) {
        this.creator = creator;
        this.sender = sender;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.value = value;
        this.localValue = localValue;
        
        this.hashCode = 17*primaryKey.hashCode() + secondaryKey.hashCode();
    }
    
    /**
     * Returns the creator of this value
     */
    public Contact getCreator() {
        return creator;
    }
    
    /**
     * Returns the sender of this value
     */
    public Contact getSender() {
        return sender;
    }
    
    /**
     * Returns the primary key of this value
     */
    public KUID getKey() {
        return primaryKey;
    }
    
    /**
     * Returns the secondary key of this value
     */
    public KUID getSecondaryKey() {
        return secondaryKey;
    }
    
    /**
     * Returns the value
     */
    public DHTValue getValue() {
        return value;
    }
    
    /**
     * This is an unsupported operation and throws thus
     * an UnsupportedOperationException
     */
    public DHTValue setValue(DHTValue value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the creation time
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Returns the time when this value was published
     */
    public long getPublishTime() {
        return publishTime;
    }
    
    /**
     * Returns the number of locations where this value is stored
     */
    public int getLocationCount() {
        return locationCount;
    }
    
    /**
     * Sets the number of locations where this value is stored
     */
    public void setLocationCount(int locationCount) {
        if (locationCount < 0) {
            throw new IllegalArgumentException("locations: " + locationCount);
        }
        
        if (!isLocalValue()) {
            return;
        }
        
        this.locationCount = locationCount;
        this.publishTime = System.currentTimeMillis();
    }
    
    /**
     * Returns true if this is a local value
     */
    public boolean isLocalValue() {
        return localValue;
    }
    
    /**
     * Returns true if this value was stored directly
     * by the creator of the value (that means creator
     * and sender are the same).
     */
    public boolean isDirect() {
        if (isLocalValue()) {
            return true;
        }
        
        return creator.equals(sender);
    }
    
    /**
     * Returns true if this value needs to be republished
     */
    public boolean isRepublishingRequired() {
        if (!isLocalValue()) {
            return false;
        }
        
        if (publishTime == 0L) {
            return true;
        }
        
        return DatabaseUtils.isRepublishingRequired(publishTime, locationCount);
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValueEntity)) {
            return false;
        }
        
        DHTValueEntity other = (DHTValueEntity)o;
        return primaryKey.equals(other.primaryKey)
                    && secondaryKey.equals(other.secondaryKey);
    }
    
    /**
     * Creates a new DHTValueEntity with the given new creator
     * if this is a local value
     */
    public DHTValueEntity changeCreator(Contact creator) {
        if (!isLocalValue()) {
            throw new UnsupportedOperationException();
        }
        
        return new DHTValueEntity(
                creator, creator, primaryKey, secondaryKey, value, true);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Creator: ").append(creator).append("\n");
        buffer.append("Sender: ").append(sender).append("\n");
        buffer.append("Primary Key: ").append(primaryKey).append("\n");
        buffer.append("Secondary Key: ").append(secondaryKey).append("\n");
        buffer.append("Local: ").append(localValue).append("\n");
        buffer.append("Locations: ").append(locationCount).append("\n");
        buffer.append("Creation time: ").append(creationTime).append("\n");
        buffer.append("Publish time: ").append(publishTime).append("\n");
        buffer.append("---\n").append(value).append("\n");
        return buffer.toString();
    }
}
