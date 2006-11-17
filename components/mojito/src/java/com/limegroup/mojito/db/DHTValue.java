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

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.Contact;

/**
 * The DHTValue class represents a <key, value> tuple that
 * is stored on the DHT. Besides the actual <key, value> tuple 
 * it's also storing the originator of the DHTValue as well as
 * the sender of the DHTValue.
 */
public interface DHTValue {
    
    /**
     * An empty value
     */
    public static final byte[] EMPTY_DATA = new byte[0];
    
    /** 
     * Returns the ValueID
     */
    public KUID getValueID();
    
    /**
     * Returns the Type of the Value
     */
    public DHTValueType getValueType();
    
    /**
     * Returns the version of this DHT value
     */
    public int getVersion();
    
    /** 
     * Returns the Value. Beware: The returned byte array is 
     * <b>NOT</b> a copy!
     */
    public byte[] getData();
    
    /** 
     * Returns the size of the value 
     */
    public int size();
    
    /** 
     * Returns whether or not the value is empty
     */
    public boolean isEmpty();
    
    /** 
     * Returns the creator of the value 
     */
    public Contact getCreator();
    
    /**
     * Returns the Node ID of the value creator
     */
    public KUID getCreatorID();
    
    /** 
     * Returns the sender of the value 
     */
    public Contact getSender();
    
    /** 
     * Returns the creationTime of this DHTValue object 
     */ 
    public long getCreationTime();
    
    /**
     * Returns the time when this DHTValue was republished
     */
    public long getPublishTime();
    
    /** 
     * Returns whether or not the originator and sender 
     * of the DHTValue are the same
     */
    public boolean isDirect();
    
    /** 
     * Returns whether or not this is a local DHTValue 
     */
    public boolean isLocalValue();
    
    /**
     * Returns true if this DHTValue requires republishing. Returns
     * always false if this is a non-local value.
     */
    public boolean isRepublishingRequired();
    
    /**
     * Sets the number of locations where this DHTValue was stored and 
     * the lastRepublishingTime to the current System time
     */
    public void setLocationCount(int locationCount);
    
    /**
     * Returns the number of locations where this DHTValue was stored
     */
    public int getLocationCount();
}
