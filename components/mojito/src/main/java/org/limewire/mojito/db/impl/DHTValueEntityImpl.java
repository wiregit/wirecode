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

package org.limewire.mojito.db.impl;

import java.util.Collection;
import java.util.Collections;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.routing.Contact;

/**
 * The default implementation of DHTValueEntity
 */
public class DHTValueEntityImpl implements DHTValueEntity {
    
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
    private volatile DHTValue value;
    
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
    private volatile transient long publishTime = 0L;
    
    /**
     * The number of locations where this value was stored
     */
    private volatile transient Collection<? extends Contact> locations;
    
    /**
     * The hash code of this entity
     */
    private final int hashCode;
    
    /**
     * 
     */
    public DHTValueEntityImpl(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value, boolean localValue) {
        this(creator, sender, primaryKey, creator.getNodeID(), value, localValue);
    }
    
    /**
     * 
     */
    private DHTValueEntityImpl(Contact creator, Contact sender, 
            KUID primaryKey, KUID secondaryKey, DHTValue value, boolean localValue) {
        this.creator = creator;
        this.sender = sender;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.value = value;
        this.localValue = localValue;
        
        this.hashCode = 17*primaryKey.hashCode() + secondaryKey.hashCode();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getCreator()
     */
    public Contact getCreator() {
        return creator;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getSender()
     */
    public Contact getSender() {
        return sender;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getKey()
     */
    public KUID getKey() {
        return primaryKey;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getSecondaryKey()
     */
    public KUID getSecondaryKey() {
        return secondaryKey;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getValue()
     */
    public DHTValue getValue() {
        return value;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#setValue(org.limewire.mojito.db.DHTValue)
     */
    public DHTValue setValue(DHTValue value) {
        if (value == null) {
            throw new NullPointerException("DHTValue is null");
        }
        
        DHTValue old = this.value;
        
        this.value = value;
        this.publishTime = 0L;
        this.locations = null;
        
        return old;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getCreationTime()
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getPublishTime()
     */
    public long getPublishTime() {
        return publishTime;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#getLocations()
     */
    public Collection<? extends Contact> getLocations() {
        Collection<? extends Contact> locations = this.locations;
        if (locations == null) {
            locations = Collections.emptySet();
        }
        return locations;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#setLocations(java.util.Collection)
     */
    public void setLocations(Collection<? extends Contact> locations) {
        if (!isLocalValue()) {
            return;
        }
        
        this.locations = locations;
        this.publishTime = System.currentTimeMillis();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#hasBeenPublished()
     */
    public boolean hasBeenPublished() {
        return localValue && publishTime > 0L; 
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#isLocalValue()
     */
    public boolean isLocalValue() {
        return localValue;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntity#isDirect()
     */
    public boolean isDirect() {
        if (isLocalValue()) {
            return true;
        }
        
        return creator.equals(sender);
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return hashCode;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValueEntity)) {
            return false;
        }
        
        DHTValueEntity other = (DHTValueEntity)o;
        return primaryKey.equals(other.getKey())
                    && secondaryKey.equals(other.getSecondaryKey());
    }
    
    /**
     * Creates a new DHTValueEntity with the given new creator
     * if this is a local value
     */
    public DHTValueEntity changeCreator(Contact creator) {
        if (!isLocalValue()) {
            throw new UnsupportedOperationException();
        }
        
        return new DHTValueEntityImpl(
                creator, creator, primaryKey, creator.getNodeID(), value, true);
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Creator: ").append(getCreator()).append("\n");
        buffer.append("Sender: ").append(getSender()).append("\n");
        buffer.append("Primary Key: ").append(getKey()).append("\n");
        buffer.append("Secondary Key: ").append(getSecondaryKey()).append("\n");
        buffer.append("Local: ").append(isLocalValue()).append("\n");
        buffer.append("Locations: ").append(getLocations()).append("\n");
        buffer.append("Creation time: ").append(getCreationTime()).append("\n");
        buffer.append("Publish time: ").append(getPublishTime()).append("\n");
        buffer.append("---\n").append(getValue()).append("\n");
        return buffer.toString();
    }
}
